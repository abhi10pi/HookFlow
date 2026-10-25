package Backend.HookFlow.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "delivery_attempts")
@Getter
@Setter
@NoArgsConstructor
public class DeliveryAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_id", nullable = false)
    private Long deliveryId;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "response_ms")
    private Long responseMs;

    @Column(columnDefinition = "text")
    private String error;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt = Instant.now();
}
