package Backend.HookFlow.messaging;

import Backend.HookFlow.config.RabbitConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryPublisher {

    private final RabbitTemplate rabbitTemplate;

    @PostConstruct
    void registerConfirmCallback() {
        rabbitTemplate.setConfirmCallback((correlation, ack, cause) -> {
            if (!ack) {
                String id = correlation != null ? correlation.getId() : "unknown";
                log.error("RabbitMQ rejected the message for delivery {}: {}", id, cause);
            }
        });
    }

    /** New deliveries: publish after the transaction that created them has committed. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDeliveriesCreated(DeliveriesCreatedEvent event) {
        for (Long deliveryId : event.deliveryIds()) {
            try {
                publish(deliveryId);
            } catch (AmqpException e) {
                // Already committed. Do not fail the sender. The reconciler will publish it later.
                log.error("Could not publish delivery {} (the reconciler will recover it)", deliveryId, e);
            }
        }
    }

    /** Publishes one delivery id. Throws AmqpException if the broker cannot be reached. */
    public void publish(long deliveryId) {
        String message = String.valueOf(deliveryId);
        rabbitTemplate.convertAndSend(
                RabbitConfig.EXCHANGE,
                RabbitConfig.ROUTING_KEY,
                message,
                new CorrelationData(message));
    }
}