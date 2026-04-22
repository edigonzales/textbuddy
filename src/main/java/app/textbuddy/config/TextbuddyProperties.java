package app.textbuddy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;

@ConfigurationProperties(prefix = "textbuddy")
public class TextbuddyProperties {

    private final Auth auth = new Auth();
    private final Observability observability = new Observability();
    private final Runtime runtime = new Runtime();

    public Auth getAuth() {
        return auth;
    }

    public Observability getObservability() {
        return observability;
    }

    public Runtime getRuntime() {
        return runtime;
    }

    public static class Auth {

        private boolean enabled;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Observability {

        private boolean usageLoggingEnabled = true;
        private String pseudonymSalt = "textbuddy";

        public boolean isUsageLoggingEnabled() {
            return usageLoggingEnabled;
        }

        public void setUsageLoggingEnabled(boolean usageLoggingEnabled) {
            this.usageLoggingEnabled = usageLoggingEnabled;
        }

        public String getPseudonymSalt() {
            return pseudonymSalt;
        }

        public void setPseudonymSalt(String pseudonymSalt) {
            this.pseudonymSalt = pseudonymSalt == null ? "" : pseudonymSalt.trim();
        }
    }

    public static class Runtime {

        private String home = "";
        private boolean initializeLocalResources = true;

        public String getHome() {
            return home;
        }

        public void setHome(String home) {
            this.home = normalize(home);
        }

        public boolean isInitializeLocalResources() {
            return initializeLocalResources;
        }

        public void setInitializeLocalResources(boolean initializeLocalResources) {
            this.initializeLocalResources = initializeLocalResources;
        }

        public Path normalizedHomePath() {
            String configuredHome = normalize(home);
            String fallbackHome = normalize(System.getProperty("user.home"));
            String resolvedFallbackHome = fallbackHome.isBlank() ? "." : fallbackHome;
            String resolvedHome = configuredHome.isBlank()
                    ? Path.of(resolvedFallbackHome, ".textbuddy").toString()
                    : configuredHome;

            try {
                return Path.of(resolvedHome).toAbsolutePath().normalize();
            } catch (InvalidPathException exception) {
                throw new IllegalStateException("textbuddy.runtime.home ist kein gültiger Pfad.", exception);
            }
        }

        public List<Path> requiredDirectories() {
            Path homePath = normalizedHomePath();
            return List.of(
                    homePath,
                    homePath.resolve("config"),
                    homePath.resolve("logs"),
                    homePath.resolve("tmp"),
                    homePath.resolve("cache")
            );
        }

        private String normalize(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
