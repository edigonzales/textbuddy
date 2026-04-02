package app.textbuddy.quickaction;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuickActionSsePayloadFactoryTest {

    @Test
    void createsChunkAndCompletePayloadsFromText() {
        QuickActionSsePayloadFactory factory = new QuickActionSsePayloadFactory();

        assertThat(factory.chunk("Teil 1").text()).isEqualTo("Teil 1");
        assertThat(factory.complete("Fertig").text()).isEqualTo("Fertig");
    }

    @Test
    void fallsBackToDefaultMessageForBlankErrors() {
        QuickActionSsePayloadFactory factory = new QuickActionSsePayloadFactory();

        assertThat(factory.error("   ").message())
                .isEqualTo("Quick Action konnte nicht abgeschlossen werden.");
    }
}
