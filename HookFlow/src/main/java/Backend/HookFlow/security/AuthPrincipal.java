package Backend.HookFlow.security;

import Backend.HookFlow.entity.Role;

/** The logged-in user, rebuilt from the JWT on every request. */
public record AuthPrincipal(Long userId, Long tenantId, String email, Role role) {
}