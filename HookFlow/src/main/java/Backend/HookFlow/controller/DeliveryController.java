package Backend.HookFlow.controller;

import Backend.HookFlow.dto.DeliveryAttemptResponse;
import Backend.HookFlow.repository.DeliveryAttemptRepository;
import Backend.HookFlow.repository.DeliveryRepository;
import Backend.HookFlow.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/deliveries")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryAttemptRepository attemptRepository;

    @GetMapping("/{id}/attempts")
    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','VIEWER')")
    public List<DeliveryAttemptResponse> attempts(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id) {
        // Verify the delivery exists (tenant isolation is via event ownership checked at event level)
        deliveryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found"));
        return attemptRepository.findByDeliveryIdOrderByAttemptNoAsc(id)
                .stream().map(DeliveryAttemptResponse::from).toList();
    }
}
