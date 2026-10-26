package Backend.HookFlow.controller;

import Backend.HookFlow.dto.CreateEndpointRequest;
import Backend.HookFlow.dto.EndpointCreatedResponse;
import Backend.HookFlow.dto.EndpointResponse;
import Backend.HookFlow.dto.PageResponse;
import Backend.HookFlow.dto.UpdateEndpointRequest;
import Backend.HookFlow.security.AuthPrincipal;
import Backend.HookFlow.service.EndpointService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/endpoints")
@RequiredArgsConstructor
public class EndpointController {

    private final EndpointService endpointService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN')")
    public EndpointCreatedResponse create(@AuthenticationPrincipal AuthPrincipal principal,
                                          @Valid @RequestBody CreateEndpointRequest request) {
        return endpointService.create(principal, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','VIEWER')")
    public PageResponse<EndpointResponse> list(@AuthenticationPrincipal AuthPrincipal principal,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return endpointService.list(principal, page, size);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','VIEWER')")
    public EndpointResponse get(@AuthenticationPrincipal AuthPrincipal principal,
                                @PathVariable Long id) {
        return endpointService.get(principal, id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN')")
    public EndpointResponse update(@AuthenticationPrincipal AuthPrincipal principal,
                                   @PathVariable Long id,
                                   @Valid @RequestBody UpdateEndpointRequest request) {
        return endpointService.update(principal, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN')")
    public void delete(@AuthenticationPrincipal AuthPrincipal principal,
                       @PathVariable Long id) {
        endpointService.deactivate(principal, id);
    }
}