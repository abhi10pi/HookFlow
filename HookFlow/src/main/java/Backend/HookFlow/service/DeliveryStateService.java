package Backend.HookFlow.service;

import Backend.HookFlow.entity.Delivery;
import Backend.HookFlow.entity.DeliveryAttempt;
import Backend.HookFlow.entity.DeliveryStatus;
import Backend.HookFlow.entity.Endpoint;
import Backend.HookFlow.entity.Event;
import Backend.HookFlow.repository.DeliveryAttemptRepository;
import Backend.HookFlow.repository.DeliveryRepository;
import Backend.HookFlow.repository.EndpointRepository;
import Backend.HookFlow.repository.EventRepository;
import Backend.HookFlow.scheduling.RetryPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryStateService {

    /** Must be longer than the longest HTTP call (10 s request timeout) plus some margin. */
    private static final Duration LEASE = Duration.ofSeconds(60);

    /** Everything the sender needs. The toString hides the secret on purpose. */
    public record DeliveryClaim(Long deliveryId, int attemptNo,
                                Long eventId, String eventType, String payload, Instant eventCreatedAt,
                                Long endpointId, String url, String secret, boolean endpointActive) {
        @Override
        public String toString() {
            return "DeliveryClaim[deliveryId=" + deliveryId + ", attemptNo=" + attemptNo + "]";
        }
    }

    /** What happened to the delivery after an attempt. */
    public record ResultOutcome(Kind kind, Instant nextRetryAt) {
        public enum Kind { SUCCESS, RETRY, DEAD }
    }

    private final DeliveryRepository deliveryRepository;
    private final DeliveryAttemptRepository attemptRepository;
    private final EventRepository eventRepository;
    private final EndpointRepository endpointRepository;
    private final RetryPolicy retryPolicy;

    /** Short transaction: win the delivery (with a lease), then load what is needed to send it. */
    @Transactional
    public Optional<DeliveryClaim> claim(long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId).orElse(null);
        if (delivery == null) {
            log.warn("Delivery {} not found, dropping the message", deliveryId);
            return Optional.empty();
        }
        if (delivery.getStatus() != DeliveryStatus.PENDING) {
            log.info("Delivery {} is already {}, skipping", deliveryId, delivery.getStatus());
            return Optional.empty();
        }

        Instant now = Instant.now();
        if (delivery.getNextRetryAt() != null && delivery.getNextRetryAt().isAfter(now)) {
            log.info("Delivery {} is not due yet or is being processed, skipping", deliveryId);
            return Optional.empty();
        }

        int expected = delivery.getAttemptCount();
        int won = deliveryRepository.claim(deliveryId, DeliveryStatus.PENDING, expected, now, now.plus(LEASE));
        if (won == 0) {
            log.info("Delivery {} was claimed by another worker, skipping", deliveryId);
            return Optional.empty();
        }

        Event event = eventRepository.findById(delivery.getEventId())
                .orElseThrow(() -> new IllegalStateException("Event missing for delivery " + deliveryId));
        Endpoint endpoint = endpointRepository.findById(delivery.getEndpointId())
                .orElseThrow(() -> new IllegalStateException("Endpoint missing for delivery " + deliveryId));

        return Optional.of(new DeliveryClaim(
                deliveryId, expected + 1,
                event.getId(), event.getType(), event.getPayload(), event.getCreatedAt(),
                endpoint.getId(), endpoint.getUrl(), endpoint.getSecret(), endpoint.isActive()));
    }

    /** Short transaction: write the attempt, then decide: SUCCESS, retry later, or DEAD. */
    @Transactional
    public ResultOutcome recordResult(DeliveryClaim claim, DeliverySender.SendResult result) {
        saveAttempt(claim, result.statusCode(), result.responseMs(), result.error());
        Delivery delivery = deliveryRepository.findById(claim.deliveryId()).orElseThrow();

        if (result.success()) {
            delivery.setStatus(DeliveryStatus.SUCCESS);
            delivery.setNextRetryAt(null);
            return new ResultOutcome(ResultOutcome.Kind.SUCCESS, null);
        }

        Optional<Instant> next = retryPolicy.nextRetryAt(claim.attemptNo(), Instant.now());
        if (next.isPresent()) {
            delivery.setNextRetryAt(next.get());   // status stays PENDING
            return new ResultOutcome(ResultOutcome.Kind.RETRY, next.get());
        }

        delivery.setStatus(DeliveryStatus.DEAD);
        delivery.setNextRetryAt(null);
        return new ResultOutcome(ResultOutcome.Kind.DEAD, null);
    }

    /** Short transaction: give up on a delivery that can never be sent. */
    @Transactional
    public void markDead(DeliveryClaim claim, String reason) {
        saveAttempt(claim, null, 0L, reason);
        Delivery delivery = deliveryRepository.findById(claim.deliveryId()).orElseThrow();
        delivery.setStatus(DeliveryStatus.DEAD);
        delivery.setNextRetryAt(null);
    }

    private void saveAttempt(DeliveryClaim claim, Integer statusCode, long responseMs, String error) {
        DeliveryAttempt attempt = new DeliveryAttempt();
        attempt.setDeliveryId(claim.deliveryId());
        attempt.setAttemptNo(claim.attemptNo());
        attempt.setStatusCode(statusCode);
        attempt.setResponseMs(responseMs);
        attempt.setError(error);
        attemptRepository.save(attempt);
    }
}