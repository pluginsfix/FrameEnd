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
import pluginsfix.frameend.config.FrameEndConfig;
import pluginsfix.frameend.domain.MaskedCoordinate;
import pluginsfix.frameend.domain.PlacedEgg;
import pluginsfix.frameend.hook.PlayerPointsHook;
import pluginsfix.frameend.hook.VaultEconomyHook;
import pluginsfix.frameend.storage.Storage;
import pluginsfix.frameend.text.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlacedEggManager {
    private final JavaPlugin plugin;
    private final FrameEndConfig config;
    private final Storage storage;
    private final Messages messages;
    private final VaultEconomyHook vaultHook;
    private final PlayerPointsHook pointsHook;
    private final DragonEggItemFactory itemFactory;

    private final Map<Integer, PlacedEgg> activeEggs = new ConcurrentHashMap<>();
    private BukkitTask payoutTask;

    public PlacedEggManager(JavaPlugin plugin, FrameEndConfig config, Storage storage,
                            Messages messages, VaultEconomyHook vaultHook, PlayerPointsHook pointsHook,
                            DragonEggItemFactory itemFactory) {
        this.plugin = plugin;
        this.config = config;
        this.storage = storage;
        this.messages = messages;
        this.vaultHook = vaultHook;
        this.pointsHook = pointsHook;
        this.itemFactory = itemFactory;
    }

    public void start() {
        activeEggs.clear();
        for (PlacedEgg egg : storage.loadAllPlacedEggs()) {
            activeEggs.put(egg.getId(), egg);
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
        }
        activeEggs.clear();
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

            if (broken) {
                destroyedEggIds.add(egg.getId());
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
        storage.deletePlacedEgg(found.getId());

        ItemStack droppedEgg = itemFactory.createEggItem(found.getCurrentDurability(), found.getMaxDurability(), found.getRepairCount());
        block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), droppedEgg);

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

    public Optional<PlacedEgg> findNearestOrFirstEgg(Location from) {
        if (activeEggs.isEmpty()) return Optional.empty();
        if (from.getWorld() == null) return activeEggs.values().stream().findFirst();

        String worldName = from.getWorld().getName();
        return activeEggs.values().stream()
                .filter(e -> e.getWorldName().equals(worldName))
                .min((a, b) -> {
                    double distA = Math.hypot(a.getX() - from.getBlockX(), a.getZ() - from.getBlockZ());
                    double distB = Math.hypot(b.getX() - from.getBlockX(), b.getZ() - from.getBlockZ());
                    return Double.compare(distA, distB);
                }).or(() -> activeEggs.values().stream().findFirst());
    }

    public List<PlacedEgg> getActiveEggs() {
        return new ArrayList<>(activeEggs.values());
    }
}
