package pluginsfix.frameend.domain;

import java.util.UUID;

public record DamageRecord(UUID playerUuid, String playerName, double damage) {
    public DamageRecord withAdditionalDamage(double extraDamage) {
        return new DamageRecord(playerUuid, playerName, this.damage + extraDamage);
    }
}
