package Backend.HookFlow.scheduling;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RetryQueue {

    public static final String KEY = "retry:queue";

    private final StringRedisTemplate redis;

    /** Schedule (or reschedule) a retry. Member = delivery id, score = due time in epoch milliseconds. */
    public void schedule(long deliveryId, Instant dueAt) {
        redis.opsForZSet().add(KEY, String.valueOf(deliveryId), dueAt.toEpochMilli());
    }

    /** Schedule only if the delivery is not already in the queue. Returns true if it was added. */
    public boolean scheduleIfAbsent(long deliveryId, Instant dueAt) {
        Boolean added = redis.opsForZSet().addIfAbsent(KEY, String.valueOf(deliveryId), dueAt.toEpochMilli());
        return Boolean.TRUE.equals(added);
    }

    /**
     * Returns the ids that are due and that THIS caller won. A due id is removed from the set,
     * and only the caller whose removal returns 1 may use it, so no retry is released twice.
     */
    public List<Long> claimDue(int limit, Instant now) {
        List<Long> won = new ArrayList<>();
        Set<String> due = redis.opsForZSet().rangeByScore(KEY, 0, now.toEpochMilli(), 0, limit);
        if (due == null) {
            return won;
        }
        for (String member : due) {
            Long removed = redis.opsForZSet().remove(KEY, member);
            if (removed != null && removed == 1) {
                won.add(Long.parseLong(member));
            }
        }
        return won;
    }
}