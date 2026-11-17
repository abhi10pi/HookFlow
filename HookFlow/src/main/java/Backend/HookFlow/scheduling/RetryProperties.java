package Backend.HookFlow.scheduling;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.delivery.retry")
public class RetryProperties {

    /** Wait after attempt 1, after attempt 2, and so on. */
    private List<Duration> delays = List.of(
            Duration.ofSeconds(10), Duration.ofMinutes(1), Duration.ofMinutes(10),
            Duration.ofHours(1), Duration.ofHours(6));

    /** 0.2 means each wait is randomly 80 to 120 percent of the base wait. */
    private double jitter = 0.2;
}