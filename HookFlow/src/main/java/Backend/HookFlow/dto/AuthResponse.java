package Backend.HookFlow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import Backend.HookFlow.entity.Role;

/** apiKey is only filled on register (shown once); it is null on login. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(String token, Role role, Long tenantId, String apiKey) {
}
