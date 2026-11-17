package Backend.HookFlow.scheduling;

import Backend.HookFlow.entity.Delivery;
import Backend.HookFlow.entity.DeliveryStatus;
import Backend.HookFlow.messaging.DeliveryPublisher;
import Backend.HookFlow.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryRetryScheduler {

    private static final int BATCH = 100;
    private static final Duration OVERDUE_GRACE = Duration.ofSeconds(30);

    private final RetryQueue retryQueue;
    private final DeliveryPublisher publisher;
    private final DeliveryRepository deliveryRepository;

    @Value("${app.delivery.scheduler.stranded-after:2m}")
    private Duration strandedAfter;

    /** Every second: release the retries that are due. */
    @Scheduled(fixedDelayString = "${app.delivery.scheduler.poll-ms:1000}")
    public void releaseDueRetries() {
        try {
            for (long deliveryId : retryQueue.claimDue(BATCH, Instant.now())) {
                try {
                    publisher.publish(deliveryId);
                } catch (AmqpException e) {
                    log.error("Could not re-publish delivery {} (the reconciler will recover it)", deliveryId, e);
                }
            }
        } catch (Exception e) {
            log.error("Retry poll failed (is Redis down?)", e);
        }
    }

    /** Every minute: repair what a crash or an outage left behind. */
    @Scheduled(fixedDelayString = "${app.delivery.scheduler.reconcile-ms:60000}", initialDelay = 10000)
    public void reconcile() {
        rescheduleOverdue();
        republishStranded();
    }

    /** Overdue in the database but missing from Redis: Redis was flushed or down, or a lease expired. */
    private void rescheduleOverdue() {
        try {
            Instant cutoff = Instant.now().minus(OVERDUE_GRACE);
            int added = 0;
            List<Delivery> overdue = deliveryRepository.findByStatusAndNextRetryAtBefore(DeliveryStatus.PENDING, cutoff);
            for (Delivery delivery : overdue) {
                if (retryQueue.scheduleIfAbsent(delivery.getId(), delivery.getNextRetryAt())) {
                    added++;
                }
            }
            if (added > 0) {
                log.warn("Reconciler re-scheduled {} overdue deliveries", added);
            }
        } catch (Exception e) {
            log.error("Reconciler could not re-schedule overdue deliveries", e);
        }
    }

    /** Saved in the database but their first message was never published (the RabbitMQ gap from Step 6). */
    private void republishStranded() {
        try {
            Instant cutoff = Instant.now().minus(strandedAfter);
            int published = 0;
            for (Delivery delivery : deliveryRepository.findStranded(DeliveryStatus.PENDING, cutoff)) {
                publisher.publish(delivery.getId());
                published++;
            }
            if (published > 0) {
                log.warn("Reconciler re-published {} stranded deliveries", published);
            }
        } catch (Exception e) {
            log.error("Reconciler could not re-publish stranded deliveries", e);
        }
    }
}