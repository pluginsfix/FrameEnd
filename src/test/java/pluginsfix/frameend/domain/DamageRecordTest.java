package pluginsfix.frameend.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DamageRecordTest {

    @Test
    void shouldAccumulateDamageCorrectly() {
        UUID id = UUID.randomUUID();
        DamageRecord record = new DamageRecord(id, "Player1", 50.0);

        DamageRecord updated = record.withAdditionalDamage(25.5);

        assertThat(updated.playerUuid()).isEqualTo(id);
        assertThat(updated.playerName()).isEqualTo("Player1");
        assertThat(updated.damage()).isEqualTo(75.5);
    }
}
