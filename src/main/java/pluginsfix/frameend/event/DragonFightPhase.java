package pluginsfix.frameend.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pluginsfix.frameend.config.FrameEndConfig;
import pluginsfix.frameend.config.LootEntry;
import pluginsfix.frameend.domain.DamageRecord;
import pluginsfix.frameend.loot.LootManager;
import pluginsfix.frameend.text.Messages;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DragonFightPhase {
    private final JavaPlugin plugin;
    private final FrameEndConfig config;
    private final Messages messages;
    private final LootManager lootManager;
    private final Runnable onPhaseComplete;

    private EnderDragon dragon;
    private BossBar bossBar;
    private double currentHealth;
    private final Map<UUID, DamageRecord> damageTracker = new ConcurrentHashMap<>();
    private BukkitTask bossBarUpdateTask;

    public DragonFightPhase(JavaPlugin plugin, FrameEndConfig config, Messages messages, LootManager lootManager, Runnable onPhaseComplete) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.lootManager = lootManager;
        this.onPhaseComplete = onPhaseComplete;
    }

    public void start() {
        damageTracker.clear();
        this.currentHealth = config.getDragonHealth();

        World world = Bukkit.getWorld(config.getEndWorldName());
        if (world == null) {
            onPhaseComplete.run();
            return;
        }

        Location spawnLoc = new Location(world, 0.5, 75.0, 0.5);
        dragon = (EnderDragon) world.spawnEntity(spawnLoc, EntityType.ENDER_DRAGON);
        dragon.setPhase(EnderDragon.Phase.CIRCLING);

        try {
            AttributeInstance maxHealthAttr = dragon.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (maxHealthAttr != null) {
                double safeMax = Math.min(config.getDragonHealth(), 2000.0);
                maxHealthAttr.setBaseValue(safeMax);
                dragon.setHealth(safeMax);
            }
        } catch (Exception ignored) {
        }

        try {
            if (dragon.getBossBar() != null) {
                dragon.getBossBar().setVisible(false);
            }
        } catch (Throwable ignored) {
        }

        BarColor color;
        try {
            color = BarColor.valueOf(config.getDragonBossbarColor().toUpperCase());
        } catch (Exception e) {
            color = BarColor.RED;
        }

        BarStyle style;
        try {
            style = BarStyle.valueOf(config.getDragonBossbarStyle().toUpperCase());
        } catch (Exception e) {
            style = BarStyle.SEGMENTED_10;
        }

        bossBar = Bukkit.createBossBar(Messages.colorizeString(formatBossbarTitle(currentHealth, config.getDragonHealth())), color, style);
        bossBar.setProgress(1.0);
        bossBar.setVisible(true);

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().equals(world)) {
                bossBar.addPlayer(p);
            }
        }

        bossBarUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (dragon == null || !dragon.isValid() || bossBar == null) return;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().equals(world)) {
                    if (!bossBar.getPlayers().contains(p)) {
                        bossBar.addPlayer(p);
                    }
                } else {
                    bossBar.removePlayer(p);
                }
            }
            updateBossBar();
        }, 5L, 5L);

        messages.broadcast("phase-dragon-started");
    }

    public void recordDamage(Player player, double amount) {
        if (dragon == null || !dragon.isValid()) return;

        currentHealth = Math.max(0.0, currentHealth - amount);

        damageTracker.compute(player.getUniqueId(), (uuid, existing) -> {
            if (existing == null) {
                return new DamageRecord(uuid, player.getName(), amount);
            }
            return existing.withAdditionalDamage(amount);
        });

        updateBossBar();

        if (currentHealth <= 0.0) {
            Location deathLoc = dragon.getLocation();
            dragon.setHealth(0.0);
            onDragonDeath(deathLoc);
        }
    }

    private void updateBossBar() {
        if (bossBar == null) return;
        double max = config.getDragonHealth();
        double progress = Math.max(0.0, Math.min(1.0, currentHealth / max));
        bossBar.setProgress(progress);
        bossBar.setTitle(Messages.colorizeString(formatBossbarTitle(currentHealth, max)));
    }

    private String formatBossbarTitle(double current, double max) {
        return config.getDragonBossbarTitle()
                .replace("{health}", String.format("%.0f", current))
                .replace("{max_health}", String.format("%.0f", max));
    }

    public void onDragonDeath(Location deathLocation) {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        if (bossBarUpdateTask != null) {
            bossBarUpdateTask.cancel();
            bossBarUpdateTask = null;
        }

        distributeRewardsAndDrops(deathLocation);
        onPhaseComplete.run();
    }

    private void distributeRewardsAndDrops(Location loc) {
        List<DamageRecord> records = new ArrayList<>(damageTracker.values());
        records.sort(Comparator.comparingDouble(DamageRecord::damage).reversed());

        DamageRecord top1 = !records.isEmpty() ? records.get(0) : null;
        DamageRecord top2 = records.size() > 1 ? records.get(1) : null;
        DamageRecord top3 = records.size() > 2 ? records.get(2) : null;

        rewardPlayer(top1, config.getTop1Frames());
        rewardPlayer(top2, config.getTop2Frames());
        rewardPlayer(top3, config.getTop3Frames());

        for (int i = 3; i < records.size(); i++) {
            DamageRecord rec = records.get(i);
            if (rec.damage() >= config.getMinDamageForReward()) {
                rewardPlayer(rec, config.getParticipationFrames());
            }
        }

        World world = loc.getWorld();
        if (world != null) {
            ExperienceOrb orb = (ExperienceOrb) world.spawnEntity(loc, EntityType.EXPERIENCE_ORB);
            orb.setExperience(config.getDropExpAmount());

            if (lootManager != null && lootManager.hasCustomLoot(LootManager.CATEGORY_DRAGON)) {
                for (ItemStack customItem : lootManager.getCategoryItems(LootManager.CATEGORY_DRAGON)) {
                    world.dropItemNaturally(loc, customItem.clone());
                }
            } else {
                for (LootEntry entry : config.getDragonExtraDrops()) {
                    if (entry.hasMaterial()) {
                        ItemStack item = new ItemStack(entry.material(), entry.amountMin());
                        if (entry.name() != null && !entry.name().isBlank()) {
                            ItemMeta meta = item.getItemMeta();
                            if (meta != null) {
                                meta.displayName(Messages.colorize(entry.name()));
                                item.setItemMeta(meta);
                            }
                        }
                        world.dropItemNaturally(loc, item);
                    }
                }
            }
        }

        messages.broadcast("dragon-killed",
                Messages.Placeholder.of("top1_player", top1 != null ? top1.playerName() : "—"),
                Messages.Placeholder.of("top1_damage", top1 != null ? String.format("%.0f", top1.damage()) : "0"),
                Messages.Placeholder.of("top1_frames", config.getTop1Frames()),
                Messages.Placeholder.of("top2_player", top2 != null ? top2.playerName() : "—"),
                Messages.Placeholder.of("top2_damage", top2 != null ? String.format("%.0f", top2.damage()) : "0"),
                Messages.Placeholder.of("top2_frames", config.getTop2Frames()),
                Messages.Placeholder.of("top3_player", top3 != null ? top3.playerName() : "—"),
                Messages.Placeholder.of("top3_damage", top3 != null ? String.format("%.0f", top3.damage()) : "0"),
                Messages.Placeholder.of("top3_frames", config.getTop3Frames())
        );
    }

    private void rewardPlayer(DamageRecord record, int frames) {
        if (record == null) return;
        for (String cmdTemplate : config.getRewardCommands()) {
            String cmd = cmdTemplate
                    .replace("{player}", record.playerName())
                    .replace("{frames}", String.valueOf(frames));
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } catch (Exception ignored) {
            }
        }
    }

    public void cleanup() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar = null;
        }
        if (bossBarUpdateTask != null) {
            bossBarUpdateTask.cancel();
            bossBarUpdateTask = null;
        }
        if (dragon != null && dragon.isValid()) {
            dragon.remove();
        }
    }

    public boolean isEventDragon(UUID entityUuid) {
        return dragon != null && dragon.getUniqueId().equals(entityUuid);
    }

    public void addPlayerIfActive(Player player) {
        if (bossBar != null && player.getWorld().getName().equals(config.getEndWorldName())) {
            bossBar.addPlayer(player);
        }
    }
}
