package Backend.HookFlow.messaging;

import java.util.List;

/** Published inside the database transaction; handled only after the commit succeeds. */
public record DeliveriesCreatedEvent(List<Long> deliveryIds) {
}