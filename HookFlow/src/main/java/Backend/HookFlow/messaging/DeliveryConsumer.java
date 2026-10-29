package Backend.HookFlow.messaging;

import Backend.HookFlow.config.RabbitConfig;
import Backend.HookFlow.entity.Delivery;
import Backend.HookFlow.entity.DeliveryStatus;
import Backend.HookFlow.repository.DeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryConsumer {

    private final DeliveryRepository deliveryRepository;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void handle(String message) {
        long deliveryId = parseId(message);

        Optional<Delivery> found = deliveryRepository.findById(deliveryId);
        if (found.isEmpty()) {
            log.warn("Delivery {} not found, dropping the message", deliveryId);
            return;
        }

        Delivery delivery = found.get();
        if (delivery.getStatus() != DeliveryStatus.PENDING) {
            log.info("Delivery {} is already {}, skipping", deliveryId, delivery.getStatus());
            return;
        }

        log.info("Received delivery {} (event {}, endpoint {})",
                delivery.getId(), delivery.getEventId(), delivery.getEndpointId());

        // Step 7: sign the payload and send the HTTP request to the endpoint here.
    }

    private long parseId(String message) {
        try {
            return Long.parseLong(message.trim());
        } catch (NumberFormatException e) {
            // A message that can never succeed: send it to the dead-letter queue, never retry it.
            throw new AmqpRejectAndDontRequeueException("Invalid delivery id: " + message, e);
        }
    }
}