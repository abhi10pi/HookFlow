package Backend.HookFlow.messaging;

import Backend.HookFlow.config.RabbitConfig;
import Backend.HookFlow.service.DeliveryProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeliveryConsumer {

    private final DeliveryProcessor processor;

    @RabbitListener(queues = RabbitConfig.QUEUE)
    public void handle(String message) {
        processor.process(parseId(message));
    }

    private long parseId(String message) {
        try {
            return Long.parseLong(message.trim());
        } catch (NumberFormatException e) {
            // A message that can never succeed: dead-letter it, never retry it.
            throw new AmqpRejectAndDontRequeueException("Invalid delivery id: " + message, e);
        }
    }
}