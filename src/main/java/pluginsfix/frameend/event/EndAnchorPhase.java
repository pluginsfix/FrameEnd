package pluginsfix.frameend.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pluginsfix.frameend.config.FrameEndConfig;
import pluginsfix.frameend.config.LootEntry;
import pluginsfix.frameend.domain.AnchorRarity;
import pluginsfix.frameend.text.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class EndAnchorPhase {
    private final JavaPlugin plugin;
    private final FrameEndConfig config;
    private final Messages messages;
    private final Runnable onPhaseComplete;
    private final Random random = new Random();

    private final Map<Location, ActiveAnchor> activeAnchors = new ConcurrentHashMap<>();
    private BukkitTask timerTask;
    private BukkitTask particleTask;
    private int remainingSeconds;

    public EndAnchorPhase(JavaPlugin plugin, FrameEndConfig config, Messages messages, Runnable onPhaseComplete) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.onPhaseComplete = onPhaseComplete;
    }

    public void start() {
        cleanup();
        World endWorld = Bukkit.getWorld(config.getEndWorldName());
        if (endWorld == null) {
            onPhaseComplete.run();
            return;
        }

        spawnAnchors(endWorld);

        remainingSeconds = config.getAnchorsPhaseDuration();
        messages.broadcast("phase-anchors-started");

        timerTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            remainingSeconds--;
            if (remainingSeconds <= 0 || activeAnchors.isEmpty()) {
                end();
                onPhaseComplete.run();
            }
        }, 20L, 20L);

        particleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::playAnchorParticles, 10L, 10L);
    }

    public void end() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }
        if (particleTask != null) {
            particleTask.cancel();
            particleTask = null;
        }
        cleanup();
    }

    private void cleanup() {
        for (Location loc : activeAnchors.keySet()) {
            if (loc.getWorld() != null && loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                Block b = loc.getBlock();
                if (b.getType() == Material.RESPAWN_ANCHOR || b.getType() == Material.CRYING_OBSIDIAN) {
                    b.setType(Material.AIR);
                }
            }
        }
        activeAnchors.clear();
    }

    private void spawnAnchors(World world) {
        int count = config.getAnchorsCount();
        for (int i = 0; i < count; i++) {
            Location loc = findSurfaceLocation(world, 15, 75);
            if (loc != null) {
                loc.getBlock().setType(Material.RESPAWN_ANCHOR);
                activeAnchors.put(loc.getBlock().getLocation(), new ActiveAnchor(AnchorRarity.COMMON, config.getAnchorHitsRequired()));
            }
        }

        if (random.nextDouble() <= config.getSecretRiftChance()) {
            for (int i = 0; i < config.getSecretRiftCount(); i++) {
                Location loc = findSurfaceLocation(world, 25, 80);
                if (loc != null) {
                    loc.getBlock().setType(Material.CRYING_OBSIDIAN);
                    activeAnchors.put(loc.getBlock().getLocation(), new ActiveAnchor(AnchorRarity.SECRET_RIFT, config.getSecretRiftHitsRequired()));
                }
            }
        }
    }

    private Location findSurfaceLocation(World world, int minRadius, int maxRadius) {
        for (int attempt = 0; attempt < 30; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = minRadius + random.nextDouble() * (maxRadius - minRadius);
            int x = (int) (dist * Math.cos(angle));
            int z = (int) (dist * Math.sin(angle));

            int highestY = world.getHighestBlockYAt(x, z);
            if (highestY > 40 && highestY < 120) {
                Location candidate = new Location(world, x, highestY + 1, z);
                if (candidate.clone().subtract(0, 1, 0).getBlock().getType().isSolid()) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private void playAnchorParticles() {
        for (Map.Entry<Location, ActiveAnchor> entry : activeAnchors.entrySet()) {
            Location loc = entry.getKey().clone().add(0.5, 1.2, 0.5);
            World w = loc.getWorld();
            if (w == null) continue;

            if (entry.getValue().rarity == AnchorRarity.SECRET_RIFT) {
                w.spawnParticle(Particle.DRAGON_BREATH, loc, 6, 0.3, 0.3, 0.3, 0.02);
                w.spawnParticle(Particle.END_ROD, loc, 3, 0.2, 0.4, 0.2, 0.01);
            } else {
                w.spawnParticle(Particle.PORTAL, loc, 8, 0.3, 0.3, 0.3, 0.05);
                w.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 2, 0.2, 0.2, 0.2, 0.01);
            }
        }
    }

    public boolean handleInteract(Player player, Block block) {
        ActiveAnchor anchor = activeAnchors.get(block.getLocation());
        if (anchor == null) return false;

        anchor.hitsLeft--;
        World world = block.getWorld();
        Location center = block.getLocation().add(0.5, 0.5, 0.5);

        world.spawnParticle(Particle.CRIT, center, 10, 0.3, 0.3, 0.3, 0.1);
        world.playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 1.2f);

        if (anchor.hitsLeft <= 0) {
            activeAnchors.remove(block.getLocation());
            block.setType(Material.AIR);

            world.spawnParticle(Particle.EXPLOSION_EMITTER, center, 1);
            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);

            distributeLoot(player, center, anchor.rarity);

            if (anchor.rarity == AnchorRarity.SECRET_RIFT) {
                messages.broadcast("secret-rift-broken", Messages.Placeholder.of("player", player.getName()));
            } else {
                messages.broadcast("anchor-broken", Messages.Placeholder.of("player", player.getName()));
            }

            if (activeAnchors.isEmpty()) {
                end();
                onPhaseComplete.run();
            }
        }
        return true;
    }

    private void distributeLoot(Player player, Location location, AnchorRarity rarity) {
        List<LootEntry> lootList = (rarity == AnchorRarity.SECRET_RIFT) ? config.getSecretRiftLoot() : config.getCommonAnchorLoot();
        World world = location.getWorld();
        if (world == null) return;

        for (LootEntry entry : lootList) {
            if (random.nextDouble() <= entry.chance()) {
                if (entry.hasMaterial()) {
                    int count = entry.amountMin() == entry.amountMax() ? entry.amountMin() :
                            entry.amountMin() + random.nextInt(entry.amountMax() - entry.amountMin() + 1);
                    ItemStack stack = new ItemStack(entry.material(), count);
                    if (entry.name() != null && !entry.name().isBlank()) {
                        ItemMeta meta = stack.getItemMeta();
                        if (meta != null) {
                            meta.displayName(Messages.colorize(entry.name()));
                            stack.setItemMeta(meta);
                        }
                    }
                    world.dropItemNaturally(location, stack);
                }
                if (entry.hasCommand()) {
                    String cmd = entry.command().replace("{player}", player.getName());
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            }
        }
    }

    public boolean isAnchorBlock(Location location) {
        return activeAnchors.containsKey(location);
    }

    private static final class ActiveAnchor {
        private final AnchorRarity rarity;
        private int hitsLeft;

        public ActiveAnchor(AnchorRarity rarity, int hitsLeft) {
            this.rarity = rarity;
            this.hitsLeft = hitsLeft;
        }
    }
}
