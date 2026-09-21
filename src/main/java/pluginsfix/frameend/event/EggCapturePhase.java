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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import pluginsfix.frameend.animation.AnimationUtil;
import pluginsfix.frameend.config.FrameEndConfig;
import pluginsfix.frameend.egg.DragonEggItemFactory;
import pluginsfix.frameend.egg.EggPickaxeItemFactory;
import pluginsfix.frameend.hologram.HologramManager;
import pluginsfix.frameend.text.Messages;

import java.util.Comparator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EggCapturePhase {
    private final JavaPlugin plugin;
    private final FrameEndConfig config;
    private final Messages messages;
    private final HologramManager hologramManager;
    private final DragonEggItemFactory itemFactory;
    private final EggPickaxeItemFactory pickaxeFactory;
    private final Runnable onPhaseComplete;
    private final Random random = new Random();

    private Location eggLocation;
    private int currentHits;
    private final Map<UUID, Integer> hitCounts = new ConcurrentHashMap<>();
    private UUID eggCarrierUuid;
    private BukkitTask carrierBroadcastTask;
    private BukkitTask eggBeaconTask;

    public EggCapturePhase(JavaPlugin plugin, FrameEndConfig config, Messages messages,
                           HologramManager hologramManager, DragonEggItemFactory itemFactory,
                           EggPickaxeItemFactory pickaxeFactory, Runnable onPhaseComplete) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.hologramManager = hologramManager;
        this.itemFactory = itemFactory;
        this.pickaxeFactory = pickaxeFactory;
        this.onPhaseComplete = onPhaseComplete;
    }

    public void start() {
        hitCounts.clear();
        eggCarrierUuid = null;
        currentHits = 0;

        World world = Bukkit.getWorld(config.getEndWorldName());
        if (world == null) {
            onPhaseComplete.run();
            return;
        }

        eggLocation = findEggSpawnLocation(world);
        if (eggLocation != null) {
            eggLocation.getBlock().setType(Material.DRAGON_EGG);
            world.strikeLightningEffect(eggLocation);
            world.playSound(eggLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
            hologramManager.updateCaptureEggHologram(eggLocation, 0, config.getEggHitsRequired());
        }

        messages.broadcast("phase-egg-started", Messages.Placeholder.of("max_hits", config.getEggHitsRequired()));

        eggBeaconTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (eggLocation != null && eggLocation.getWorld() != null) {
                World w = eggLocation.getWorld();
                if (w.isChunkLoaded(eggLocation.getBlockX() >> 4, eggLocation.getBlockZ() >> 4)) {
                    Block b = eggLocation.getBlock();
                    if (b.getType() != Material.DRAGON_EGG) {
                        b.setType(Material.DRAGON_EGG);
                    }
                    w.spawnParticle(Particle.END_ROD, eggLocation.clone().add(0.5, 1.0, 0.5), 15, 0.4, 0.8, 0.4, 0.05);
                    w.spawnParticle(Particle.DRAGON_BREATH, eggLocation.clone().add(0.5, 0.5, 0.5), 8, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }, 10L, 10L);
    }

    private Location findEggSpawnLocation(World world) {
        for (int i = 0; i < 20; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double dist = 10 + random.nextDouble() * 40;
            int x = (int) (dist * Math.cos(angle));
            int z = (int) (dist * Math.sin(angle));
            int y = world.getHighestBlockYAt(x, z);
            if (y > 40 && y < 100) {
                return new Location(world, x, y + 1, z);
            }
        }
        return new Location(world, 0, world.getHighestBlockYAt(0, 0) + 1, 0);
    }

    public boolean handleEggHit(Player player, Block block) {
        if (eggLocation == null || !block.getLocation().equals(eggLocation)) {
            return false;
        }

        int hitsToAdd = 1;
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (pickaxeFactory != null && pickaxeFactory.isEggBreakerPickaxe(inHand)) {
            if (player.isSneaking()) {
                hitsToAdd = Math.max(1, config.getEggHitsRequired() - currentHits);
            } else {
                hitsToAdd = Math.min(100, Math.max(1, config.getEggHitsRequired() - currentHits));
            }
            World world = block.getWorld();
            world.playSound(block.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.6f);
            world.spawnParticle(Particle.SONIC_BOOM, block.getLocation().add(0.5, 0.5, 0.5), 1);
        }

        currentHits += hitsToAdd;
        hitCounts.merge(player.getUniqueId(), hitsToAdd, Integer::sum);
        int remaining = Math.max(0, config.getEggHitsRequired() - currentHits);

        player.sendBlockChange(block.getLocation(), block.getBlockData());
        AnimationUtil.playEggHitAnimation(block.getLocation().add(0.5, 0.5, 0.5), remaining, config.getEggHitsRequired());
        hologramManager.updateCaptureEggHologram(eggLocation, currentHits, config.getEggHitsRequired());

        messages.send(player, "egg-hit-actionbar",
                Messages.Placeholder.of("remaining", remaining),
                Messages.Placeholder.of("current_hits", currentHits),
                Messages.Placeholder.of("max_hits", config.getEggHitsRequired())
        );

        if (currentHits % 50 == 0 || currentHits >= config.getEggHitsRequired() - 10) {
            messages.broadcast("egg-hit",
                    Messages.Placeholder.of("current_hits", currentHits),
                    Messages.Placeholder.of("max_hits", config.getEggHitsRequired()),
                    Messages.Placeholder.of("remaining", remaining),
                    Messages.Placeholder.of("player", player.getName())
            );
        }

        if (currentHits >= config.getEggHitsRequired()) {
            finishEggCapture();
        }
        return true;
    }

    private void finishEggCapture() {
        if (eggBeaconTask != null) {
            eggBeaconTask.cancel();
            eggBeaconTask = null;
        }

        hologramManager.remove("capture_egg");

        if (eggLocation != null) {
            eggLocation.getBlock().setType(Material.AIR);
        }

        UUID winnerUuid = hitCounts.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);

        Player winner = winnerUuid != null ? Bukkit.getPlayer(winnerUuid) : null;
        if (winner == null || !winner.isOnline()) {
            winner = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        }

        if (winner != null) {
            this.eggCarrierUuid = winner.getUniqueId();
            ItemStack dragonEggItem = itemFactory.createEggItem(config.getEggMaxDurability(), config.getEggMaxDurability(), 0);

            Map<Integer, ItemStack> overflow = winner.getInventory().addItem(dragonEggItem);
            if (!overflow.isEmpty()) {
                winner.getWorld().dropItemNaturally(winner.getLocation(), dragonEggItem);
            }

            int glowDurationTicks = config.getGlowingDurationSeconds() * 20;
            winner.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowDurationTicks, 1, false, false, true));

            if (eggLocation != null) {
                AnimationUtil.playEggCaptureAnimation(eggLocation.clone().add(0.5, 0.5, 0.5), winner);
            }

            messages.broadcast("egg-captured",
                    Messages.Placeholder.of("player", winner.getName())
            );

            startCarrierTracking(winner);
        }

        onPhaseComplete.run();
    }

    private void startCarrierTracking(Player carrier) {
        long periodTicks = config.getAnnouncementIntervalSeconds() * 20L;
        carrierBroadcastTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            Player p = Bukkit.getPlayer(eggCarrierUuid);
            if (p != null && p.isOnline()) {
                Location loc = p.getLocation();
                messages.broadcast("carrier-announcement",
                        Messages.Placeholder.of("player", p.getName()),
                        Messages.Placeholder.of("x", loc.getBlockX()),
                        Messages.Placeholder.of("y", loc.getBlockY()),
                        Messages.Placeholder.of("z", loc.getBlockZ())
                );
            }
        }, periodTicks, periodTicks);
    }

    public void cleanup() {
        if (eggBeaconTask != null) {
            eggBeaconTask.cancel();
            eggBeaconTask = null;
        }
        if (carrierBroadcastTask != null) {
            carrierBroadcastTask.cancel();
            carrierBroadcastTask = null;
        }
        hologramManager.remove("capture_egg");
        if (eggLocation != null && eggLocation.getWorld() != null) {
            Block b = eggLocation.getBlock();
            if (b.getType() == Material.DRAGON_EGG) {
                b.setType(Material.AIR);
            }
        }
    }

    public boolean isEggBlock(Location location) {
        return eggLocation != null && eggLocation.equals(location);
    }

    public boolean isEventEgg(Location location) {
        return isEggBlock(location);
    }
}
