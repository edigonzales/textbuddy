package app.textbuddy.observability;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

@Component
public class UsagePseudonymizer {

    private static final int DEFAULT_HASH_LENGTH = 16;

    public String pseudonymize(String pseudonymSalt, String userIdentifier) {
        String normalizedIdentifier = normalize(userIdentifier);

        if (normalizedIdentifier.isBlank()) {
            return "anonymous";
        }

        String normalizedSalt = normalize(pseudonymSalt);
        byte[] digest = digestSha256(normalizedSalt + "::" + normalizedIdentifier);
        String hash = HexFormat.of().formatHex(digest);
        return "u-" + hash.substring(0, Math.min(hash.length(), DEFAULT_HASH_LENGTH));
    }

    private byte[] digestSha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 ist auf dieser Runtime nicht verfügbar.", exception);
        }
    }

    private String normalize(String value) {
        return Objects.requireNonNullElse(value, "").trim();
    }
}
