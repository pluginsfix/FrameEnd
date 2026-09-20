package pluginsfix.frameend.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FrameEndConfig {
    private final JavaPlugin plugin;

    private List<DayOfWeek> scheduleDays = new ArrayList<>();
    private LocalTime scheduleTime = LocalTime.of(18, 0);
    private ZoneId timezone = ZoneId.of("Europe/Moscow");
    private List<Long> broadcastIntervals = new ArrayList<>();

    private String endWorldName = "world_the_end";
    private String spawnWorldName = "world";
    private double spawnX = 0.5;
    private double spawnY = 64.0;
    private double spawnZ = 0.5;
    private float spawnYaw = 0.0f;
    private float spawnPitch = 0.0f;
    private double endIslandRadiusLimit = 220.0;
    private int closeDelaySeconds = 180;

    private int anchorsPhaseDuration = 600;
    private int anchorsCount = 10;
    private int anchorHitsRequired = 5;
    private int secretRiftCount = 2;
    private double secretRiftChance = 0.40;
    private int secretRiftHitsRequired = 8;
    private final List<LootEntry> commonAnchorLoot = new ArrayList<>();
    private final List<LootEntry> secretRiftLoot = new ArrayList<>();

    private double dragonHealth = 3000.0;
    private String dragonBossbarTitle = "&#FB8808▶ &fДревний Эндер-Дракон &8[&#FFFF00{health}&8/&#FFFF00{max_health}&8] &fHP";
    private String dragonBossbarColor = "RED";
    private String dragonBossbarStyle = "SEGMENTED_10";
    private int top1Frames = 500;
    private int top2Frames = 300;
    private int top3Frames = 150;
    private int participationFrames = 50;
    private double minDamageForReward = 50.0;
    private List<String> rewardCommands = new ArrayList<>();
    private int dropExpAmount = 25000;
    private final List<LootEntry> dragonExtraDrops = new ArrayList<>();

    private int eggHitsRequired = 500;
    private String eggTitle = "&#FFFF00◆ &fЯйцо Древнего Дракона";
    private int glowingDurationSeconds = 180;
    private int announcementIntervalSeconds = 15;

    private int eggIncomeIntervalSeconds = 1;
    private double eggIncomeAmount = 10.0;
    private String eggIncomeCurrency = "VAULT";
    private int eggMaxDurability = 5000;
    private int eggDurabilityLossPerPayout = 1;
    private double baseRepairCostMoney = 100000.0;
    private int baseRepairCostExpLevels = 30;
    private int baseRepairCostFrames = 100;
    private double repairCostMultiplier = 1.5;
    private char maskCharacter = '*';
    private int maskDigitsCount = 2;

    private long compassCooldownSeconds = 172800L;
    private String compassItemName = "&#FFFF00▶ &fКомпас Дракона";
    private List<String> compassItemLore = new ArrayList<>();

    public FrameEndConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "config.yml");
        if (!file.exists()) {
            plugin.saveResource("config.yml", false);
        }
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        scheduleDays.clear();
        for (String day : config.getStringList("schedule.days")) {
            try {
                scheduleDays.add(DayOfWeek.valueOf(day.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
            }
        }
        if (scheduleDays.isEmpty()) {
            scheduleDays.add(DayOfWeek.WEDNESDAY);
            scheduleDays.add(DayOfWeek.SATURDAY);
        }

        String timeStr = config.getString("schedule.time-msk", "18:00");
        String[] parts = timeStr.split(":");
        if (parts.length == 2) {
            scheduleTime = LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }

        try {
            timezone = ZoneId.of(config.getString("schedule.timezone", "Europe/Moscow"));
        } catch (Exception ignored) {
            timezone = ZoneId.of("Europe/Moscow");
        }

        broadcastIntervals.clear();
        for (long sec : config.getLongList("schedule.broadcast-intervals-seconds")) {
            broadcastIntervals.add(sec);
        }
        broadcastIntervals.sort((a, b) -> Long.compare(b, a));

        endWorldName = config.getString("world-settings.end-world-name", "world_the_end");
        spawnWorldName = config.getString("world-settings.spawn-world-name", "world");
        spawnX = config.getDouble("world-settings.spawn-x", 0.5);
        spawnY = config.getDouble("world-settings.spawn-y", 64.0);
        spawnZ = config.getDouble("world-settings.spawn-z", 0.5);
        spawnYaw = (float) config.getDouble("world-settings.spawn-yaw", 0.0);
        spawnPitch = (float) config.getDouble("world-settings.spawn-pitch", 0.0);
        endIslandRadiusLimit = config.getDouble("world-settings.end-island-radius-limit", 220.0);
        closeDelaySeconds = config.getInt("world-settings.close-delay-seconds", 180);

        anchorsPhaseDuration = config.getInt("anchors-phase.duration-seconds", 600);
        anchorsCount = config.getInt("anchors-phase.anchors-count", 10);
        anchorHitsRequired = config.getInt("anchors-phase.anchor-hits-required", 5);
        secretRiftCount = config.getInt("anchors-phase.secret-rift-count", 2);
        secretRiftChance = config.getDouble("anchors-phase.secret-rift-chance", 0.40);
        secretRiftHitsRequired = config.getInt("anchors-phase.secret-rift-hits-required", 8);

        commonAnchorLoot.clear();
        loadLootList(config.getMapList("anchors-phase.common-loot"), commonAnchorLoot);

        secretRiftLoot.clear();
        loadLootList(config.getMapList("anchors-phase.secret-loot"), secretRiftLoot);

        dragonHealth = config.getDouble("dragon-phase.health", 3000.0);
        dragonBossbarTitle = config.getString("dragon-phase.bossbar-title", "&#FB8808▶ &fДревний Эндер-Дракон &8[&#FFFF00{health}&8/&#FFFF00{max_health}&8] &fHP");
        dragonBossbarColor = config.getString("dragon-phase.bossbar-color", "RED");
        dragonBossbarStyle = config.getString("dragon-phase.bossbar-style", "SEGMENTED_10");
        top1Frames = config.getInt("dragon-phase.damage-rewards.first-place-frames", 500);
        top2Frames = config.getInt("dragon-phase.damage-rewards.second-place-frames", 300);
        top3Frames = config.getInt("dragon-phase.damage-rewards.third-place-frames", 150);
        participationFrames = config.getInt("dragon-phase.damage-rewards.participation-frames", 50);
        minDamageForReward = config.getDouble("dragon-phase.damage-rewards.min-damage-for-reward", 50.0);
        rewardCommands = config.getStringList("dragon-phase.damage-rewards.reward-commands");
        dropExpAmount = config.getInt("dragon-phase.drop-exp-amount", 25000);

        dragonExtraDrops.clear();
        loadLootList(config.getMapList("dragon-phase.extra-drop-items"), dragonExtraDrops);

        eggHitsRequired = config.getInt("egg-phase.durability-hits", 500);
        eggTitle = config.getString("egg-phase.egg-title", "&#FFFF00◆ &fЯйцо Древнего Дракона");
        glowingDurationSeconds = config.getInt("egg-phase.glowing-duration-seconds", 180);
        announcementIntervalSeconds = config.getInt("egg-phase.announcement-interval-seconds", 15);

        eggIncomeIntervalSeconds = config.getInt("placed-egg.income-interval-seconds", 1);
        eggIncomeAmount = config.getDouble("placed-egg.income-amount", 10.0);
        eggIncomeCurrency = config.getString("placed-egg.income-currency", "VAULT");
        eggMaxDurability = config.getInt("placed-egg.max-durability", 5000);
        eggDurabilityLossPerPayout = config.getInt("placed-egg.durability-loss-per-payout", 1);
        baseRepairCostMoney = config.getDouble("placed-egg.base-repair-cost-money", 100000.0);
        baseRepairCostExpLevels = config.getInt("placed-egg.base-repair-cost-exp-levels", 30);
        baseRepairCostFrames = config.getInt("placed-egg.base-repair-cost-frames", 100);
        repairCostMultiplier = config.getDouble("placed-egg.repair-cost-multiplier", 1.5);
        String maskCharStr = config.getString("placed-egg.mask-character", "*");
        maskCharacter = maskCharStr.isEmpty() ? '*' : maskCharStr.charAt(0);
        maskDigitsCount = config.getInt("placed-egg.mask-digits-count", 2);

        compassCooldownSeconds = config.getLong("dragon-compass.cooldown-seconds", 172800L);
        compassItemName = config.getString("dragon-compass.item-name", "&#FFFF00▶ &fКомпас Дракона");
        compassItemLore = config.getStringList("dragon-compass.item-lore");
    }

    private void loadLootList(List<Map<?, ?>> list, List<LootEntry> target) {
        for (Map<?, ?> entry : list) {
            String matName = (String) entry.get("material");
            Material mat = null;
            if (matName != null) {
                try {
                    mat = Material.valueOf(matName.toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }

            int min = entry.containsKey("amount-min") ? ((Number) entry.get("amount-min")).intValue() :
                    (entry.containsKey("amount") ? ((Number) entry.get("amount")).intValue() : 1);
            int max = entry.containsKey("amount-max") ? ((Number) entry.get("amount-max")).intValue() : min;
            double chance = entry.containsKey("chance") ? ((Number) entry.get("chance")).doubleValue() : 1.0;
            String name = (String) entry.get("name");
            String cmd = (String) entry.get("command");

            target.add(new LootEntry(mat, min, max, chance, name, cmd));
        }
    }

    public List<DayOfWeek> getScheduleDays() { return scheduleDays; }
    public LocalTime getScheduleTime() { return scheduleTime; }
    public ZoneId getTimezone() { return timezone; }
    public List<Long> getBroadcastIntervals() { return broadcastIntervals; }
    public String getEndWorldName() { return endWorldName; }
    public String getSpawnWorldName() { return spawnWorldName; }
    public double getSpawnX() { return spawnX; }
    public double getSpawnY() { return spawnY; }
    public double getSpawnZ() { return spawnZ; }
    public float getSpawnYaw() { return spawnYaw; }
    public float getSpawnPitch() { return spawnPitch; }
    public double getEndIslandRadiusLimit() { return endIslandRadiusLimit; }
    public int getCloseDelaySeconds() { return closeDelaySeconds; }
    public int getAnchorsPhaseDuration() { return anchorsPhaseDuration; }
    public int getAnchorsCount() { return anchorsCount; }
    public int getAnchorHitsRequired() { return anchorHitsRequired; }
    public int getSecretRiftCount() { return secretRiftCount; }
    public double getSecretRiftChance() { return secretRiftChance; }
    public int getSecretRiftHitsRequired() { return secretRiftHitsRequired; }
    public List<LootEntry> getCommonAnchorLoot() { return commonAnchorLoot; }
    public List<LootEntry> getSecretRiftLoot() { return secretRiftLoot; }
    public double getDragonHealth() { return dragonHealth; }
    public String getDragonBossbarTitle() { return dragonBossbarTitle; }
    public String getDragonBossbarColor() { return dragonBossbarColor; }
    public String getDragonBossbarStyle() { return dragonBossbarStyle; }
    public int getTop1Frames() { return top1Frames; }
    public int getTop2Frames() { return top2Frames; }
    public int getTop3Frames() { return top3Frames; }
    public int getParticipationFrames() { return participationFrames; }
    public double getMinDamageForReward() { return minDamageForReward; }
    public List<String> getRewardCommands() { return rewardCommands; }
    public int getDropExpAmount() { return dropExpAmount; }
    public List<LootEntry> getDragonExtraDrops() { return dragonExtraDrops; }
    public int getEggHitsRequired() { return eggHitsRequired; }
    public String getEggTitle() { return eggTitle; }
    public int getGlowingDurationSeconds() { return glowingDurationSeconds; }
    public int getAnnouncementIntervalSeconds() { return announcementIntervalSeconds; }
    public int getEggIncomeIntervalSeconds() { return eggIncomeIntervalSeconds; }
    public double getEggIncomeAmount() { return eggIncomeAmount; }
    public String getEggIncomeCurrency() { return eggIncomeCurrency; }
    public int getEggMaxDurability() { return eggMaxDurability; }
    public int getEggDurabilityLossPerPayout() { return eggDurabilityLossPerPayout; }
    public double getBaseRepairCostMoney() { return baseRepairCostMoney; }
    public int getBaseRepairCostExpLevels() { return baseRepairCostExpLevels; }
    public int getBaseRepairCostFrames() { return baseRepairCostFrames; }
    public double getRepairCostMultiplier() { return repairCostMultiplier; }
    public char getMaskCharacter() { return maskCharacter; }
    public int getMaskDigitsCount() { return maskDigitsCount; }
    public long getCompassCooldownSeconds() { return compassCooldownSeconds; }
    public String getCompassItemName() { return compassItemName; }
    public List<String> getCompassItemLore() { return compassItemLore; }
}
