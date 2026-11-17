package Backend.HookFlow.service;

import Backend.HookFlow.common.WebhookSigner;
import Backend.HookFlow.service.DeliveryStateService.DeliveryClaim;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class DeliverySender {

    public record SendResult(boolean success, Integer statusCode, long responseMs, String error) {
    }

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_ERROR_LENGTH = 500;

    // One shared, thread-safe client. It never follows redirects.
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    private final UrlValidator urlValidator;

    /** Never throws: every problem becomes a failed SendResult. Runs with NO database transaction. */
    public SendResult send(DeliveryClaim claim) {
        long start = System.nanoTime();
        try {
            urlValidator.validate(claim.url());   // re-check at delivery time (DNS can change)

            String body = buildBody(claim);
            long timestamp = Instant.now().getEpochSecond();
            String signature = WebhookSigner.sign(claim.secret(), timestamp, body);

            HttpRequest request = HttpRequest.newBuilder(URI.create(claim.url()))
                    .timeout(REQUEST_TIMEOUT)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "HookFlow/1.0")
                    .header("X-HookFlow-Event-Id", String.valueOf(claim.eventId()))
                    .header("X-HookFlow-Delivery-Id", String.valueOf(claim.deliveryId()))
                    .header("X-HookFlow-Event-Type", claim.eventType())
                    .header("X-HookFlow-Timestamp", String.valueOf(timestamp))
                    .header("X-HookFlow-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();
            boolean ok = code >= 200 && code < 300;
            return new SendResult(ok, code, elapsedMs(start), ok ? null : "Receiver answered HTTP " + code);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new SendResult(false, null, elapsedMs(start), "Interrupted while sending");
        } catch (Exception e) {
            return new SendResult(false, null, elapsedMs(start), describe(e));
        }
    }

    /** Built once as a string; this exact string is signed and sent. */
    static String buildBody(DeliveryClaim c) {
        return """
               {"id":%d,"type":"%s","createdAt":"%s","data":%s}"""
                .formatted(c.eventId(), c.eventType(), c.eventCreatedAt(), c.payload());
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static String describe(Exception e) {
        String text = e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : "");
        return text.length() > MAX_ERROR_LENGTH ? text.substring(0, MAX_ERROR_LENGTH) : text;
    }
}