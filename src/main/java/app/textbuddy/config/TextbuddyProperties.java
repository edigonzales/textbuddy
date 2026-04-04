package app.textbuddy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "textbuddy")
public class TextbuddyProperties {

    private final Auth auth = new Auth();

    public Auth getAuth() {
        return auth;
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
}
