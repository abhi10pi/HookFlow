package Backend.HookFlow.controller;

import Backend.HookFlow.security.AuthPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** TEMPORARY: only for testing JWT and roles. Delete when Step 4 starts. */
@RestController
public class TempTestController {

    @GetMapping("/me")
    public AuthPrincipal me(@AuthenticationPrincipal AuthPrincipal principal) {
        return principal;
    }

    @GetMapping("/dev-only")
    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN')")
    public String devOnly() {
        return "developer area";
    }

    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminOnly() {
        return "admin area";
    }
}