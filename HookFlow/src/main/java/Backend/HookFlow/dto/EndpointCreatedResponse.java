package Backend.HookFlow.dto;

import Backend.HookFlow.entity.Endpoint;

import java.time.Instant;

public record EndpointCreatedResponse(Long id, String url, boolean active,
                                      Instant createdAt, String secret) {

    public static EndpointCreatedResponse from(Endpoint e) {
        return new EndpointCreatedResponse(e.getId(), e.getUrl(), e.isActive(),
                e.getCreatedAt(), e.getSecret());
    }
}