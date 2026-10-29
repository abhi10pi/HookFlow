package Backend.HookFlow.dto;

import Backend.HookFlow.entity.Delivery;
import Backend.HookFlow.entity.DeliveryStatus;

import java.time.Instant;

public record DeliveryResponse(Long id, Long endpointId, DeliveryStatus status,
                               int attemptCount, Instant nextRetryAt, Instant createdAt, Instant updatedAt) {
    public static DeliveryResponse from(Delivery d) {
        return new DeliveryResponse(d.getId(), d.getEndpointId(), d.getStatus(),
                d.getAttemptCount(), d.getNextRetryAt(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
