package Backend.HookFlow.dto;

import Backend.HookFlow.entity.DeliveryAttempt;

import java.time.Instant;

public record DeliveryAttemptResponse(Long id, int attemptNo, Integer statusCode,
                                      Long responseMs, String error, Instant attemptedAt) {
    public static DeliveryAttemptResponse from(DeliveryAttempt a) {
        return new DeliveryAttemptResponse(a.getId(), a.getAttemptNo(), a.getStatusCode(),
                a.getResponseMs(), a.getError(), a.getAttemptedAt());
    }
}
