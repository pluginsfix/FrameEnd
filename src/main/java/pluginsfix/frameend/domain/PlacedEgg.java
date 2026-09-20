package pluginsfix.frameend.domain;

import java.util.UUID;

public final class PlacedEgg {
    private final int id;
    private final UUID ownerUuid;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;
    private int currentDurability;
    private final int maxDurability;
    private int repairCount;
    private final long placedTime;

    public PlacedEgg(int id, UUID ownerUuid, String worldName, int x, int y, int z,
                     int currentDurability, int maxDurability, int repairCount, long placedTime) {
        this.id = id;
        this.ownerUuid = ownerUuid;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.currentDurability = currentDurability;
        this.maxDurability = maxDurability;
        this.repairCount = repairCount;
        this.placedTime = placedTime;
    }

    public int getId() {
        return id;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public int getCurrentDurability() {
        return currentDurability;
    }

    public int getMaxDurability() {
        return maxDurability;
    }

    public int getRepairCount() {
        return repairCount;
    }

    public long getPlacedTime() {
        return placedTime;
    }

    public boolean reduceDurability(int amount) {
        this.currentDurability = Math.max(0, this.currentDurability - amount);
        return this.currentDurability <= 0;
    }

    public void repair(int restoredDurability) {
        this.currentDurability = Math.min(this.maxDurability, this.currentDurability + restoredDurability);
        this.repairCount++;
    }

    public double calculateMoneyRepairCost(double baseCost, double multiplier) {
        return baseCost * Math.pow(multiplier, repairCount);
    }

    public int calculateExpRepairCost(int baseExp, double multiplier) {
        return (int) Math.round(baseExp * Math.pow(multiplier, repairCount));
    }

    public int calculateFramesRepairCost(int baseFrames, double multiplier) {
        return (int) Math.round(baseFrames * Math.pow(multiplier, repairCount));
    }
}
