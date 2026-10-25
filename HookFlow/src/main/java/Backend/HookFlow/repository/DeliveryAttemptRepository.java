package Backend.HookFlow.repository;

import Backend.HookFlow.entity.DeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, Long> {
    List<DeliveryAttempt> findByDeliveryIdOrderByAttemptNoAsc(Long deliveryId);
}
