package Backend.HookFlow.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Component
@RequiredArgsConstructor
public class RetryPolicy {

    private final RetryProperties properties;

    public int maxAttempts() {
        return properties.getDelays().size() + 1;
    }

    /**
     * @param attemptsMade how many attempts have been made so far, including the one that just failed
     * @return when to try again, or empty when there are no attempts left (the delivery becomes DEAD)
     */
    public Optional<Instant> nextRetryAt(int attemptsMade, Instant now) {
        List<Duration> delays = properties.getDelays();
        if (attemptsMade < 1 || attemptsMade > delays.size()) {
            return Optional.empty();
        }
        long baseMillis = delays.get(attemptsMade - 1).toMillis();
        return Optional.of(now.plusMillis(withJitter(baseMillis)));
    }

    private long withJitter(long baseMillis) {
        double jitter = Math.max(0.0, Math.min(properties.getJitter(), 0.9));
        if (jitter == 0.0) {
            return baseMillis;
        }
        double factor = 1.0 + ThreadLocalRandom.current().nextDouble(-jitter, jitter);
        return Math.round(baseMillis * factor);
    }
}