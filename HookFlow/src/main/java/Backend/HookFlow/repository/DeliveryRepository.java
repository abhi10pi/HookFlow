package Backend.HookFlow.repository;

import Backend.HookFlow.entity.Delivery;
import Backend.HookFlow.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByEventId(Long eventId);
    List<Delivery> findByStatusAndNextRetryAtBefore(DeliveryStatus status, Instant time);
}
