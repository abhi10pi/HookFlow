package Backend.HookFlow.controller;

import Backend.HookFlow.dto.CreateEventRequest;
import Backend.HookFlow.dto.EventAcceptedResponse;
import Backend.HookFlow.dto.EventSummaryResponse;
import Backend.HookFlow.dto.DeliveryResponse;
import Backend.HookFlow.dto.PageResponse;
import Backend.HookFlow.entity.Delivery;
import Backend.HookFlow.entity.Event;
import Backend.HookFlow.repository.DeliveryRepository;
import Backend.HookFlow.repository.EventRepository;
import Backend.HookFlow.security.ApiClientPrincipal;
import Backend.HookFlow.security.AuthPrincipal;
import Backend.HookFlow.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventRepository eventRepository;
    private final DeliveryRepository deliveryRepository;

    @PostMapping
    public ResponseEntity<EventAcceptedResponse> ingest(
            @AuthenticationPrincipal ApiClientPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateEventRequest request) {

        EventAcceptedResponse body = eventService.ingest(principal.tenantId(), request, idempotencyKey);
        HttpStatus status = body.duplicate() ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(body);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','VIEWER')")
    public PageResponse<EventSummaryResponse> list(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<Event> p = eventRepository.findByTenantId(
                principal.tenantId(),
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.of(p.map(EventSummaryResponse::from));
    }

    @GetMapping("/{id}/deliveries")
    @PreAuthorize("hasAnyRole('DEVELOPER','ADMIN','VIEWER')")
    public List<DeliveryResponse> deliveries(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable Long id) {
        eventRepository.findByIdAndTenantId(id, principal.tenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        return deliveryRepository.findByEventId(id).stream()
                .map(DeliveryResponse::from)
                .toList();
    }
}
