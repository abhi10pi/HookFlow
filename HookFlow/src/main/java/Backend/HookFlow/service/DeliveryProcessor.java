package Backend.HookFlow.service;

import Backend.HookFlow.scheduling.RetryPolicy;
import Backend.HookFlow.scheduling.RetryQueue;
import Backend.HookFlow.service.DeliveryStateService.DeliveryClaim;
import Backend.HookFlow.service.DeliveryStateService.ResultOutcome;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryProcessor {

    private final DeliveryStateService stateService;
    private final DeliverySender sender;
    private final RetryQueue retryQueue;
    private final RetryPolicy retryPolicy;

    // NOT @Transactional on purpose: no database transaction is open while we wait for the receiver.
    public void process(long deliveryId) {
        Optional<DeliveryClaim> claimed = stateService.claim(deliveryId);
        if (claimed.isEmpty()) {
            return;   // missing, finished, not due yet, or another worker owns it
        }
        DeliveryClaim claim = claimed.get();

        if (!claim.endpointActive()) {
            stateService.markDead(claim, "Endpoint is inactive");
            log.info("Delivery {} marked DEAD: endpoint {} is inactive", deliveryId, claim.endpointId());
            return;
        }

        DeliverySender.SendResult result = sender.send(claim);
        ResultOutcome outcome = stateService.recordResult(claim, result);   // committed when this returns

        switch (outcome.kind()) {
            case SUCCESS -> log.info("Delivery {} attempt {}: SUCCESS (HTTP {}, {} ms)",
                    deliveryId, claim.attemptNo(), result.statusCode(), result.responseMs());
            case RETRY -> {
                log.warn("Delivery {} attempt {}: FAILED ({}, {} ms). Next try at {}",
                        deliveryId, claim.attemptNo(), result.error(), result.responseMs(), outcome.nextRetryAt());
                scheduleRetry(deliveryId, outcome.nextRetryAt());
            }
            case DEAD -> log.error("Delivery {} is DEAD after {} attempts (last error: {})",
                    deliveryId, claim.attemptNo(), result.error());
        }
    }

    /** Redis is only an index. If this fails, the database still holds the due time and the reconciler repairs it. */
    private void scheduleRetry(long deliveryId, Instant dueAt) {
        try {
            retryQueue.schedule(deliveryId, dueAt);
        } catch (Exception e) {
            log.error("Could not schedule the retry of delivery {} in Redis (the reconciler will recover it)",
                    deliveryId, e);
        }
    }
}