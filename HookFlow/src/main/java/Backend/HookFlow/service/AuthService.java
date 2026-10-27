package Backend.HookFlow.service;

import Backend.HookFlow.dto.AuthResponse;
import Backend.HookFlow.dto.LoginRequest;
import Backend.HookFlow.dto.RegisterRequest;
import Backend.HookFlow.entity.Role;
import Backend.HookFlow.entity.Tenant;
import Backend.HookFlow.entity.User;
import Backend.HookFlow.repository.TenantRepository;
import Backend.HookFlow.repository.UserRepository;
import Backend.HookFlow.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import Backend.HookFlow.common.HashUtil;
import java.security.SecureRandom;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        // The API key is shown ONCE. Only its hash is stored.
        String apiKey = generateApiKey();

        Tenant tenant = new Tenant();
        tenant.setName(req.tenantName().trim());
        // tenant.setApiKeyHash(sha256(apiKey));
        tenant.setApiKeyHash(HashUtil.sha256(apiKey));
        tenantRepository.save(tenant);

        User user = new User();
        user.setTenantId(tenant.getId());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setRole(Role.DEVELOPER);
        userRepository.save(user);

        return new AuthResponse(jwtService.generate(user), user.getRole(), tenant.getId(), apiKey);
    }

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.email().trim().toLowerCase())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        return new AuthResponse(jwtService.generate(user), user.getRole(), user.getTenantId(), null);
    }

    private static String generateApiKey() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return "hr_" + HexFormat.of().formatHex(bytes);
    }

}