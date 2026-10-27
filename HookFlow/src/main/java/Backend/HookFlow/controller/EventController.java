package Backend.HookFlow.controller;

import Backend.HookFlow.dto.CreateEventRequest;
import Backend.HookFlow.dto.EventAcceptedResponse;
import Backend.HookFlow.security.ApiClientPrincipal;
import Backend.HookFlow.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventAcceptedResponse> ingest(
            @AuthenticationPrincipal ApiClientPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreateEventRequest request) {

        EventAcceptedResponse body = eventService.ingest(principal.tenantId(), request, idempotencyKey);
        HttpStatus status = body.duplicate() ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(body);
    }
}