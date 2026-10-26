package Backend.HookFlow.service;

import Backend.HookFlow.common.SecretGenerator;
import Backend.HookFlow.dto.CreateEndpointRequest;
import Backend.HookFlow.dto.EndpointCreatedResponse;
import Backend.HookFlow.dto.EndpointResponse;
import Backend.HookFlow.dto.PageResponse;
import Backend.HookFlow.dto.UpdateEndpointRequest;
import Backend.HookFlow.entity.Endpoint;
import Backend.HookFlow.repository.EndpointRepository;
import Backend.HookFlow.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EndpointService {

    private static final int MAX_ACTIVE_ENDPOINTS = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final EndpointRepository endpointRepository;
    private final UrlValidator urlValidator;

    @Transactional
    public EndpointCreatedResponse create(AuthPrincipal principal, CreateEndpointRequest req) {
        String url = req.url().trim();
        urlValidator.validate(url);
        ensureUnderLimit(principal.tenantId());

        Endpoint endpoint = new Endpoint();
        endpoint.setTenantId(principal.tenantId());   // from the TOKEN, never from the request
        endpoint.setUrl(url);
        endpoint.setSecret(SecretGenerator.webhookSecret());
        endpointRepository.save(endpoint);

        return EndpointCreatedResponse.from(endpoint);
    }

    @Transactional(readOnly = true)
    public PageResponse<EndpointResponse> list(AuthPrincipal principal, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "id"));

        return PageResponse.of(
                endpointRepository.findByTenantId(principal.tenantId(), pageable)
                        .map(EndpointResponse::from));
    }

    @Transactional(readOnly = true)
    public EndpointResponse get(AuthPrincipal principal, Long id) {
        return EndpointResponse.from(find(principal, id));
    }

    @Transactional
    public EndpointResponse update(AuthPrincipal principal, Long id, UpdateEndpointRequest req) {
        Endpoint endpoint = find(principal, id);

        if (req.url() != null) {
            String url = req.url().trim();
            urlValidator.validate(url);
            endpoint.setUrl(url);
        }
        if (req.active() != null) {
            if (req.active() && !endpoint.isActive()) {
                ensureUnderLimit(principal.tenantId());   // re-activating counts against the limit
            }
            endpoint.setActive(req.active());
        }
        return EndpointResponse.from(endpoint);   // changes are saved automatically at commit
    }

    @Transactional
    public void deactivate(AuthPrincipal principal, Long id) {
        find(principal, id).setActive(false);
    }

    private Endpoint find(AuthPrincipal principal, Long id) {
        return endpointRepository.findByIdAndTenantId(id, principal.tenantId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Endpoint not found"));
    }

    private void ensureUnderLimit(Long tenantId) {
        if (endpointRepository.countByTenantIdAndActiveTrue(tenantId) >= MAX_ACTIVE_ENDPOINTS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Endpoint limit reached (" + MAX_ACTIVE_ENDPOINTS + " active endpoints)");
        }
    }
}