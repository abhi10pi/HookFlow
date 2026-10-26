package Backend.HookFlow.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

@Component
public class UrlValidator {

    private final boolean allowPrivateAddresses;

    public UrlValidator(@Value("${app.webhook.allow-private-addresses:false}") boolean allowPrivateAddresses) {
        this.allowPrivateAddresses = allowPrivateAddresses;
    }

    public void validate(String rawUrl) {
        URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (URISyntaxException e) {
            throw bad("Invalid URL");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw bad("URL must start with http:// or https://");
        }
        if (uri.getHost() == null) {
            throw bad("URL must contain a host");
        }
        if (uri.getUserInfo() != null) {
            throw bad("URL must not contain credentials");
        }
        if (allowPrivateAddresses) {
            return;
        }

        try {
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (isInternal(address)) {
                    throw bad("URL points to a private or internal address");
                }
            }
        } catch (UnknownHostException e) {
            throw bad("Host cannot be resolved");
        }
    }

    private static boolean isInternal(InetAddress a) {
        if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress()
                || a.isSiteLocalAddress() || a.isMulticastAddress()) {
            return true;
        }
        byte[] b = a.getAddress();
        // IPv6 unique local addresses (fc00::/7)
        return b.length == 16 && (b[0] & 0xFE) == 0xFC;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}