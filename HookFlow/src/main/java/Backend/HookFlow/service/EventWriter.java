package Backend.HookFlow.service;

import Backend.HookFlow.entity.Delivery;
import Backend.HookFlow.entity.Endpoint;
import Backend.HookFlow.entity.Event;
import Backend.HookFlow.repository.DeliveryRepository;
import Backend.HookFlow.repository.EndpointRepository;
import Backend.HookFlow.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventWriter {

    public record SavedEvent(Event event, int deliveryCount) {
    }

    private final EventRepository eventRepository;
    private final EndpointRepository endpointRepository;
    private final DeliveryRepository deliveryRepository;

    /** Saves the event and one PENDING delivery per active endpoint, all in ONE transaction. */
    @Transactional
    public SavedEvent save(Long tenantId, String type, String payloadJson, String idempotencyKey) {
        Event event = new Event();
        event.setTenantId(tenantId);
        event.setType(type);
        event.setPayload(payloadJson);
        event.setIdempotencyKey(idempotencyKey);
        eventRepository.saveAndFlush(event);   // flush now so a duplicate key fails right here

        List<Delivery> deliveries = endpointRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                .map(endpoint -> newDelivery(event, endpoint))
                .toList();
        deliveryRepository.saveAll(deliveries);

        return new SavedEvent(event, deliveries.size());
    }

    private static Delivery newDelivery(Event event, Endpoint endpoint) {
        Delivery delivery = new Delivery();
        delivery.setEventId(event.getId());
        delivery.setEndpointId(endpoint.getId());
        return delivery;   // status defaults to PENDING, attemptCount to 0
    }
}