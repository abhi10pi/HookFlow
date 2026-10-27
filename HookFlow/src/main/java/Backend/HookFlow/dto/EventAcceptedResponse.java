package Backend.HookFlow.dto;

import Backend.HookFlow.entity.Event;

import java.time.Instant;

public record EventAcceptedResponse(Long eventId, String type, int deliveries,
                                    boolean duplicate, Instant createdAt) {

    public static EventAcceptedResponse of(Event e, int deliveries, boolean duplicate) {
        return new EventAcceptedResponse(e.getId(), e.getType(), deliveries, duplicate, e.getCreatedAt());
    }
}