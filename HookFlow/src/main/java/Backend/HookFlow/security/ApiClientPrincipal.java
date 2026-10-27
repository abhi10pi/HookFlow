package Backend.HookFlow.security;

/** A machine caller authenticated by API key. It belongs to exactly one tenant. */
public record ApiClientPrincipal(Long tenantId) {
}