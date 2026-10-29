package Backend.HookFlow.dto;

import Backend.HookFlow.entity.Event;

import java.time.Instant;

public record EventSummaryResponse(Long id, String type, String idempotencyKey, Instant createdAt) {
    public static EventSummaryResponse from(Event e) {
        return new EventSummaryResponse(e.getId(), e.getType(), e.getIdempotencyKey(), e.getCreatedAt());
    }
}
