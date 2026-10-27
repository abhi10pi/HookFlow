package Backend.HookFlow.service;

import Backend.HookFlow.dto.CreateEventRequest;
import Backend.HookFlow.dto.EventAcceptedResponse;
import Backend.HookFlow.entity.Event;
import Backend.HookFlow.repository.DeliveryRepository;
import Backend.HookFlow.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EventService {

    private static final int MAX_PAYLOAD_BYTES = 256 * 1024;
    private static final int MAX_KEY_LENGTH = 100;

    private final EventWriter eventWriter;
    private final EventRepository eventRepository;
    private final DeliveryRepository deliveryRepository;
    private final ObjectMapper objectMapper;

    // NOT @Transactional on purpose: the transaction lives inside EventWriter.save
    public EventAcceptedResponse ingest(Long tenantId, CreateEventRequest req, String rawIdempotencyKey) {
        String key = normalizeKey(rawIdempotencyKey);

        if (key != null) {
            Optional<Event> existing = eventRepository.findByTenantIdAndIdempotencyKey(tenantId, key);
            if (existing.isPresent()) {
                return duplicate(existing.get());
            }
        }

        String payloadJson = serialize(req.payload());

        try {
            EventWriter.SavedEvent saved = eventWriter.save(tenantId, req.type(), payloadJson, key);
            return EventAcceptedResponse.of(saved.event(), saved.deliveryCount(), false);
        } catch (DataIntegrityViolationException e) {
            // Lost a race: another request with the same key committed first.
            if (key == null) {
                throw e;
            }
            return eventRepository.findByTenantIdAndIdempotencyKey(tenantId, key)
                    .map(this::duplicate)
                    .orElseThrow(() -> e);
        }
    }

    private EventAcceptedResponse duplicate(Event event) {
        int deliveries = deliveryRepository.findByEventId(event.getId()).size();
        return EventAcceptedResponse.of(event, deliveries, true);
    }

    private String normalizeKey(String raw) {
        if (raw == null) {
            return null;
        }
        String key = raw.trim();
        if (key.isEmpty() || key.length() > MAX_KEY_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key must be 1-" + MAX_KEY_LENGTH + " characters");
        }
        return key;
    }

    private String serialize(Map<String, Object> payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload is not valid JSON");
        }
        if (json.getBytes(StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE,
                    "Payload must be at most " + (MAX_PAYLOAD_BYTES / 1024) + " KB");
        }
        return json;
    }
}