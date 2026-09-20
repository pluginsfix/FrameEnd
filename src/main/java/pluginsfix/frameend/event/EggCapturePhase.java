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
import pluginsfix.frameend.config.FrameEndConfig;
import pluginsfix.frameend.egg.DragonEggItemFactory;
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
    private final DragonEggItemFactory itemFactory;
    private final Runnable onPhaseComplete;
    private final Random random = new Random();

    private Location eggLocation;
    private int currentHits;
    private final Map<UUID, Integer> hitCounts = new ConcurrentHashMap<>();
    private UUID eggCarrierUuid;
    private BukkitTask carrierBroadcastTask;
    private BukkitTask eggBeaconTask;

    public EggCapturePhase(JavaPlugin plugin, FrameEndConfig config, Messages messages,
                           DragonEggItemFactory itemFactory, Runnable onPhaseComplete) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.itemFactory = itemFactory;
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
        }

        messages.broadcast("phase-egg-started");

        eggBeaconTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (eggLocation != null && eggLocation.getBlock().getType() == Material.DRAGON_EGG) {
                World w = eggLocation.getWorld();
                if (w != null) {
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

        currentHits++;
        hitCounts.merge(player.getUniqueId(), 1, Integer::sum);

        World world = block.getWorld();
        world.spawnParticle(Particle.CRIT, block.getLocation().add(0.5, 0.5, 0.5), 6, 0.2, 0.2, 0.2, 0.1);
        world.playSound(block.getLocation(), Sound.BLOCK_STONE_BREAK, 1.0f, 1.5f);

        if (currentHits % 50 == 0 || currentHits >= config.getEggHitsRequired() - 10) {
            messages.broadcast("egg-hit",
                    Messages.Placeholder.of("current_hits", currentHits),
                    Messages.Placeholder.of("max_hits", config.getEggHitsRequired()),
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
        if (eggLocation != null && eggLocation.getBlock().getType() == Material.DRAGON_EGG) {
            eggLocation.getBlock().setType(Material.AIR);
        }
    }

    public boolean isEventEgg(Location loc) {
        return eggLocation != null && eggLocation.equals(loc);
    }
}
