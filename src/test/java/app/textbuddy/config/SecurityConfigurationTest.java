package app.textbuddy.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigurationTest {

    @Test
    void openModeRequiresAnExplicitLoopbackBinding() {
        assertThatThrownBy(() -> SecurityConfiguration.validateOpenDevelopmentMode(false, new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Loopback-Adresse");
        assertThatThrownBy(() -> SecurityConfiguration.validateOpenDevelopmentMode(
                false,
                new MockEnvironment().withProperty("server.address", "0.0.0.0")
        )).isInstanceOf(IllegalStateException.class);

        assertThatCode(() -> SecurityConfiguration.validateOpenDevelopmentMode(
                false,
                new MockEnvironment().withProperty("server.address", "127.0.0.1")
        )).doesNotThrowAnyException();
    }

    @Test
    void protectedModeMayBindToTheServerInterface() {
        assertThatCode(() -> SecurityConfiguration.validateOpenDevelopmentMode(true, new MockEnvironment()))
                .doesNotThrowAnyException();
    }
}
