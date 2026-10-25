package Backend.HookFlow.repository;

import Backend.HookFlow.entity.Endpoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EndpointRepository extends JpaRepository<Endpoint, Long> {
    Page<Endpoint> findByTenantId(Long tenantId, Pageable pageable);
    List<Endpoint> findByTenantIdAndActiveTrue(Long tenantId);
    Optional<Endpoint> findByIdAndTenantId(Long id, Long tenantId);
}
