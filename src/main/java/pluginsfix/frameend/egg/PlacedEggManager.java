package pluginsfix.frameend.egg;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pluginsfix.frameend.animation.AnimationUtil;
import pluginsfix.frameend.config.FrameEndConfig;
import pluginsfix.frameend.domain.MaskedCoordinate;
import pluginsfix.frameend.domain.PlacedEgg;
import pluginsfix.frameend.hologram.HologramManager;
import pluginsfix.frameend.hook.PlayerPointsHook;
import pluginsfix.frameend.hook.VaultEconomyHook;
import pluginsfix.frameend.storage.Storage;
import pluginsfix.frameend.text.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class PlacedEggManager {
    private final JavaPlugin plugin;
    private final FrameEndConfig config;
    private final Storage storage;
    private final Messages messages;
    private final VaultEconomyHook vaultHook;
    private final PlayerPointsHook pointsHook;
    private final DragonEggItemFactory itemFactory;
    private final HologramManager hologramManager;

    private final Map<Integer, PlacedEgg> activeEggs = new ConcurrentHashMap<>();
    private BukkitTask payoutTask;

    public PlacedEggManager(JavaPlugin plugin, FrameEndConfig config, Storage storage,
                            Messages messages, VaultEconomyHook vaultHook, PlayerPointsHook pointsHook,
                            DragonEggItemFactory itemFactory, HologramManager hologramManager) {
        this.plugin = plugin;
        this.config = config;
        this.storage = storage;
        this.messages = messages;
        this.vaultHook = vaultHook;
        this.pointsHook = pointsHook;
        this.itemFactory = itemFactory;
        this.hologramManager = hologramManager;
    }

    public void start() {
        activeEggs.clear();
        for (PlacedEgg egg : storage.loadAllPlacedEggs()) {
            activeEggs.put(egg.getId(), egg);
            updateEggHologram(egg);
        }

        long periodTicks = Math.max(20L, config.getEggIncomeIntervalSeconds() * 20L);
        payoutTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processPayoutTick, periodTicks, periodTicks);
    }

    public void stop() {
        if (payoutTask != null) {
            payoutTask.cancel();
            payoutTask = null;
        }
        for (PlacedEgg egg : activeEggs.values()) {
            storage.updatePlacedEgg(egg.getId(), egg.getCurrentDurability(), egg.getRepairCount());
            hologramManager.remove("placed_egg_" + egg.getId());
        }
        activeEggs.clear();
    }

    private void updateEggHologram(PlacedEgg egg) {
        World world = Bukkit.getWorld(egg.getWorldName());
        if (world == null) return;

        Location loc = new Location(world, egg.getX(), egg.getY(), egg.getZ());
        OfflinePlayer owner = Bukkit.getOfflinePlayer(egg.getOwnerUuid());
        String ownerName = owner.getName() != null ? owner.getName() : "Неизвестно";

        hologramManager.updatePlacedEggHologram(
                egg.getId(),
                loc,
                ownerName,
                egg.getCurrentDurability(),
                egg.getMaxDurability(),
                config.getEggIncomeAmount(),
                config.getEggIncomeCurrency()
        );
    }

    private void processPayoutTick() {
        List<Integer> destroyedEggIds = new ArrayList<>();

        for (PlacedEgg egg : activeEggs.values()) {
            World world = Bukkit.getWorld(egg.getWorldName());
            if (world == null) continue;

            if (world.isChunkLoaded(egg.getX() >> 4, egg.getZ() >> 4)) {
                Block block = world.getBlockAt(egg.getX(), egg.getY(), egg.getZ());
                if (block.getType() != Material.DRAGON_EGG) {
                    destroyedEggIds.add(egg.getId());
                    continue;
                }
            }

            OfflinePlayer owner = Bukkit.getOfflinePlayer(egg.getOwnerUuid());
            double income = config.getEggIncomeAmount();

            if ("POINTS".equalsIgnoreCase(config.getEggIncomeCurrency())) {
                pointsHook.givePoints(egg.getOwnerUuid(), (int) Math.round(income));
            } else {
                vaultHook.deposit(owner, income);
            }

            if (owner.isOnline()) {
                Player player = owner.getPlayer();
                if (player != null) {
                    messages.send(player, "placed-egg-income-received",
                            Messages.Placeholder.of("amount", String.format("%.1f", income)),
                            Messages.Placeholder.of("durability", egg.getCurrentDurability()),
                            Messages.Placeholder.of("max_durability", egg.getMaxDurability())
                    );
                }
            }

            boolean broken = egg.reduceDurability(config.getEggDurabilityLossPerPayout());
            storage.updatePlacedEgg(egg.getId(), egg.getCurrentDurability(), egg.getRepairCount());
            updateEggHologram(egg);

            if (broken) {
                destroyedEggIds.add(egg.getId());
                Location loc = new Location(world, egg.getX() + 0.5, egg.getY() + 0.5, egg.getZ() + 0.5);
                AnimationUtil.playPlacedEggBreakAnimation(loc);

                if (world.isChunkLoaded(egg.getX() >> 4, egg.getZ() >> 4)) {
                    Block block = world.getBlockAt(egg.getX(), egg.getY(), egg.getZ());
                    if (block.getType() == Material.DRAGON_EGG) {
                        block.setType(Material.AIR);
                    }
                }
                messages.broadcast("placed-egg-destroyed-wear");
            }
        }

        for (int id : destroyedEggIds) {
            activeEggs.remove(id);
            hologramManager.remove("placed_egg_" + id);
            storage.deletePlacedEgg(id);
        }
    }

    public void onEggPlaced(Player player, Block block, ItemStack item) {
        int durability = itemFactory.getDurability(item, config.getEggMaxDurability());
        int maxDurability = itemFactory.getMaxDurability(item, config.getEggMaxDurability());
        int repairCount = itemFactory.getRepairCount(item);

        int id = storage.insertPlacedEgg(player.getUniqueId(), block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ(),
                durability, maxDurability, repairCount, System.currentTimeMillis());

        if (id > 0) {
            PlacedEgg placedEgg = new PlacedEgg(id, player.getUniqueId(), block.getWorld().getName(),
                    block.getX(), block.getY(), block.getZ(),
                    durability, maxDurability, repairCount, System.currentTimeMillis());
            activeEggs.put(id, placedEgg);
            updateEggHologram(placedEgg);

            String maskedX = MaskedCoordinate.mask(block.getX(), config.getMaskDigitsCount(), config.getMaskCharacter());
            String maskedY = MaskedCoordinate.mask(block.getY(), 1, config.getMaskCharacter());
            String maskedZ = MaskedCoordinate.mask(block.getZ(), config.getMaskDigitsCount(), config.getMaskCharacter());

            messages.broadcast("placed-egg-broadcast",
                    Messages.Placeholder.of("player", player.getName()),
                    Messages.Placeholder.of("masked_x", maskedX),
                    Messages.Placeholder.of("masked_y", maskedY),
                    Messages.Placeholder.of("masked_z", maskedZ)
            );
        }
    }

    public boolean onEggBroken(Player player, Block block) {
        PlacedEgg found = getEggAt(block.getLocation()).orElse(null);
        if (found == null) {
            return false;
        }

        activeEggs.remove(found.getId());
        hologramManager.remove("placed_egg_" + found.getId());
        storage.deletePlacedEgg(found.getId());

        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        AnimationUtil.playPlacedEggBreakAnimation(center);

        ItemStack droppedEgg = itemFactory.createEggItem(found.getCurrentDurability(), found.getMaxDurability(), found.getRepairCount());
        block.getWorld().dropItemNaturally(center, droppedEgg);

        messages.broadcast("placed-egg-broken",
                Messages.Placeholder.of("player", player.getName())
        );
        return true;
    }

    public Optional<PlacedEgg> getEggAt(Location location) {
        if (location.getWorld() == null) return Optional.empty();
        String worldName = location.getWorld().getName();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        return activeEggs.values().stream()
                .filter(e -> e.getWorldName().equals(worldName) && e.getX() == x && e.getY() == y && e.getZ() == z)
                .findFirst();
    }

    public void updateEggAfterRepair(PlacedEgg egg) {
        storage.updatePlacedEgg(egg.getId(), egg.getCurrentDurability(), egg.getRepairCount());
        updateEggHologram(egg);
    }

    public List<PlacedEgg> getActiveEggs() {
        return new ArrayList<>(activeEggs.values());
    }

    public Optional<PlacedEgg> findNearestOrFirstEgg(Location loc) {
        if (activeEggs.isEmpty()) return Optional.empty();
        if (loc.getWorld() == null) return activeEggs.values().stream().findFirst();

        String worldName = loc.getWorld().getName();
        return activeEggs.values().stream()
                .filter(e -> e.getWorldName().equals(worldName))
                .min(java.util.Comparator.comparingDouble(e -> {
                    double dx = loc.getX() - (e.getX() + 0.5);
                    double dy = loc.getY() - (e.getY() + 0.5);
                    double dz = loc.getZ() - (e.getZ() + 0.5);
                    return dx * dx + dy * dy + dz * dz;
                }))
                .or(() -> activeEggs.values().stream().findFirst());
    }

    public int getPlacedEggsCount() {
        return activeEggs.size();
    }
}
