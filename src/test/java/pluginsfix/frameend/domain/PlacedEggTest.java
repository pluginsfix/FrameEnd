package pluginsfix.frameend.domain;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PlacedEggTest {

    @Test
    void shouldReduceDurabilityCorrectly() {
        PlacedEgg egg = new PlacedEgg(1, UUID.randomUUID(), "world", 100, 64, -200, 500, 500, 0, System.currentTimeMillis());

        boolean isBroken = egg.reduceDurability(100);
        assertThat(isBroken).isFalse();
        assertThat(egg.getCurrentDurability()).isEqualTo(400);

        boolean brokenAfterDeplete = egg.reduceDurability(400);
        assertThat(brokenAfterDeplete).isTrue();
        assertThat(egg.getCurrentDurability()).isEqualTo(0);
    }

    @Test
    void shouldRepairAndIncrementRepairCount() {
        PlacedEgg egg = new PlacedEgg(1, UUID.randomUUID(), "world", 100, 64, -200, 50, 500, 0, System.currentTimeMillis());

        egg.repair(500);

        assertThat(egg.getCurrentDurability()).isEqualTo(500);
        assertThat(egg.getRepairCount()).isEqualTo(1);
    }

    @Test
    void shouldScaleRepairCostsProgressively() {
        PlacedEgg egg = new PlacedEgg(1, UUID.randomUUID(), "world", 100, 64, -200, 0, 500, 2, System.currentTimeMillis());

        double moneyCost = egg.calculateMoneyRepairCost(1000.0, 1.5);
        int expCost = egg.calculateExpRepairCost(10, 1.5);
        int framesCost = egg.calculateFramesRepairCost(20, 1.5);

        assertThat(moneyCost).isEqualTo(2250.0);
        assertThat(expCost).isEqualTo(23);
        assertThat(framesCost).isEqualTo(45);
    }
}
