package Backend.HookFlow.service;

import Backend.HookFlow.entity.Delivery;
import Backend.HookFlow.entity.Endpoint;
import Backend.HookFlow.entity.Event;
import Backend.HookFlow.messaging.DeliveriesCreatedEvent;
import Backend.HookFlow.repository.DeliveryRepository;
import Backend.HookFlow.repository.EndpointRepository;
import Backend.HookFlow.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

    /** Saves the event and one PENDING delivery per active endpoint, all in ONE transaction. */
    @Transactional
    public SavedEvent save(Long tenantId, String type, String payloadJson, String idempotencyKey) {
        Event event = new Event();
        event.setTenantId(tenantId);
        event.setType(type);
        event.setPayload(payloadJson);
        event.setIdempotencyKey(idempotencyKey);
        eventRepository.saveAndFlush(event);

        List<Delivery> deliveries = endpointRepository.findByTenantIdAndActiveTrue(tenantId).stream()
                .map(endpoint -> newDelivery(event, endpoint))
                .toList();
        List<Delivery> saved = deliveryRepository.saveAll(deliveries);

        if (!saved.isEmpty()) {
            List<Long> ids = saved.stream().map(Delivery::getId).toList();
            // Handled by DeliveryPublisher, but ONLY after this transaction commits.
            eventPublisher.publishEvent(new DeliveriesCreatedEvent(ids));
        }

        return new SavedEvent(event, saved.size());
    }

    private static Delivery newDelivery(Event event, Endpoint endpoint) {
        Delivery delivery = new Delivery();
        delivery.setEventId(event.getId());
        delivery.setEndpointId(endpoint.getId());
        return delivery;
    }
}