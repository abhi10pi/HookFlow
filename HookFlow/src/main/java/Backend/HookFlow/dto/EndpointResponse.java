package Backend.HookFlow.dto;

import Backend.HookFlow.entity.Endpoint;

import java.time.Instant;

public record EndpointResponse(Long id, String url, boolean active, Instant createdAt) {

    public static EndpointResponse from(Endpoint e) {
        return new EndpointResponse(e.getId(), e.getUrl(), e.isActive(), e.getCreatedAt());
    }
}