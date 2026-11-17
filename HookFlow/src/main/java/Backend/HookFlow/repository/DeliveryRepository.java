package Backend.HookFlow.repository;

import Backend.HookFlow.entity.Delivery;
import Backend.HookFlow.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    List<Delivery> findByEventId(Long eventId);

    /** Overdue retries and expired leases. */
    List<Delivery> findByStatusAndNextRetryAtBefore(DeliveryStatus status, Instant time);

    /** Deliveries that were saved but whose first message was never published. */
    @Query("select d from Delivery d where d.status = :status and d.attemptCount = 0 "
            + "and d.nextRetryAt is null and d.createdAt < :before")
    List<Delivery> findStranded(@Param("status") DeliveryStatus status, @Param("before") Instant before);

    /**
     * Atomic claim with a lease. Returns 1 if this caller won the delivery, 0 if another worker did.
     * attemptCount acts as a version number. next_retry_at is set to the lease expiry time.
     */
    @Modifying(clearAutomatically = true)
    @Query("update Delivery d set d.attemptCount = d.attemptCount + 1, d.nextRetryAt = :leaseUntil, "
            + "d.updatedAt = :now where d.id = :id and d.status = :status and d.attemptCount = :expected")
    int claim(@Param("id") Long id,
              @Param("status") DeliveryStatus status,
              @Param("expected") int expected,
              @Param("now") Instant now,
              @Param("leaseUntil") Instant leaseUntil);
}