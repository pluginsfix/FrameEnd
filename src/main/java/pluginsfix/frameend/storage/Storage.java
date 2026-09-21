package pluginsfix.frameend.storage;

import pluginsfix.frameend.domain.PlacedEgg;

import java.util.List;
import java.util.UUID;

public interface Storage {
    void init();
    void close();
    int insertPlacedEgg(UUID ownerUuid, String worldName, int x, int y, int z, int currentDurability, int maxDurability, int repairCount, long placedTime, String auraType);
    void updatePlacedEgg(int id, int currentDurability, int repairCount);
    void updatePlacedEggAura(int id, String auraType);
    void deletePlacedEgg(int id);
    List<PlacedEgg> loadAllPlacedEggs();
    long getCompassCooldown(UUID playerUuid);
    void setCompassCooldown(UUID playerUuid, long timestampMillis);
}
