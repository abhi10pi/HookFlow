package Backend.HookFlow.service;

import Backend.HookFlow.service.DeliverySender.SendResult;
import Backend.HookFlow.service.DeliveryStateService.DeliveryClaim;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryProcessor {

    private final DeliveryStateService stateService;
    private final DeliverySender sender;

    // NOT @Transactional on purpose: no database transaction is open while we wait for the receiver.
    public void process(long deliveryId) {
        Optional<DeliveryClaim> claimed = stateService.claim(deliveryId);
        if (claimed.isEmpty()) {
            return;   // missing, already finished, or another worker owns it
        }
        DeliveryClaim claim = claimed.get();

        if (!claim.endpointActive()) {
            stateService.markDead(claim, "Endpoint is inactive");
            log.info("Delivery {} marked DEAD: endpoint {} is inactive", deliveryId, claim.endpointId());
            return;
        }

        DeliverySender.SendResult result = sender.send(claim);
        stateService.recordResult(claim, result);

        if (result.success()) {
            log.info("Delivery {} attempt {}: SUCCESS (HTTP {}, {} ms)",
                    deliveryId, claim.attemptNo(), result.statusCode(), result.responseMs());
        } else {
            log.warn("Delivery {} attempt {}: FAILED ({}, {} ms)",
                    deliveryId, claim.attemptNo(), result.error(), result.responseMs());
        }
    }
}