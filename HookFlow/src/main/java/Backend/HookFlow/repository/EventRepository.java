package Backend.HookFlow.repository;

import Backend.HookFlow.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {
    Optional<Event> findByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey);
    Optional<Event> findByIdAndTenantId(Long id, Long tenantId);
}
