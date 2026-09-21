package pluginsfix.frameend.egg;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import pluginsfix.frameend.animation.AnimationUtil;
import pluginsfix.frameend.config.FrameEndConfig;
import pluginsfix.frameend.domain.MaskedCoordinate;
import pluginsfix.frameend.domain.PlacedEgg;
import pluginsfix.frameend.hologram.HologramManager;
import pluginsfix.frameend.hook.PlayerPointsHook;
import pluginsfix.frameend.hook.VaultEconomyHook;
import pluginsfix.frameend.storage.Storage;
import pluginsfix.frameend.telegram.TelegramNotifier;
import pluginsfix.frameend.text.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
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
    private final TelegramNotifier telegramNotifier;
    private final Random random = new Random();

    private final Map<Integer, PlacedEgg> activeEggs = new ConcurrentHashMap<>();
    private final Map<Integer, List<Entity>> sentinelsMap = new ConcurrentHashMap<>();
    private final Map<Integer, Long> lastAlertTime = new ConcurrentHashMap<>();

    private BukkitTask payoutTask;
    private BukkitTask auraTask;
    private BukkitTask cosmeticParticleTask;
    private BukkitTask eclipseTask;
    private boolean isEclipseActive = false;
    private double particleAngle = 0;

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
        this.telegramNotifier = new TelegramNotifier(config.isTelegramEnabled(), config.getTelegramBotToken(), config.getTelegramChatId());
    }

    public void start() {
        activeEggs.clear();
        for (PlacedEgg egg : storage.loadAllPlacedEggs()) {
            activeEggs.put(egg.getId(), egg);
            updateEggHologram(egg);
            spawnSentinels(egg);
        }

        long periodTicks = Math.max(20L, config.getEggIncomeIntervalSeconds() * 20L);
        payoutTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processPayoutTick, periodTicks, periodTicks);

        if (config.isEggAuraEnabled()) {
            auraTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processAuraTick, 20L, 20L);
        }

        cosmeticParticleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processCosmeticParticles, 5L, 5L);

        if (config.isEclipseEnabled()) {
            startEclipseSchedule();
        }
    }

    public void stop() {
        if (payoutTask != null) {
            payoutTask.cancel();
            payoutTask = null;
        }
        if (auraTask != null) {
            auraTask.cancel();
            auraTask = null;
        }
        if (cosmeticParticleTask != null) {
            cosmeticParticleTask.cancel();
            cosmeticParticleTask = null;
        }
        if (eclipseTask != null) {
            eclipseTask.cancel();
            eclipseTask = null;
        }

        for (PlacedEgg egg : activeEggs.values()) {
            storage.updatePlacedEgg(egg.getId(), egg.getCurrentDurability(), egg.getRepairCount());
            hologramManager.remove("placed_egg_" + egg.getId());
            cleanupSentinels(egg.getId());
        }
        activeEggs.clear();
        sentinelsMap.clear();
    }

    private void startEclipseSchedule() {
        long intervalTicks = Math.max(200L, config.getEclipseIntervalSeconds() * 20L);
        eclipseTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            isEclipseActive = true;
            messages.broadcast("eclipse-started");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                isEclipseActive = false;
                messages.broadcast("eclipse-ended");
            }, config.getEclipseDurationSeconds() * 20L);
        }, intervalTicks, intervalTicks);
    }

    private void updateEggHologram(PlacedEgg egg) {
        World world = Bukkit.getWorld(egg.getWorldName());
        if (world == null) return;

        Location loc = new Location(world, egg.getX(), egg.getY(), egg.getZ());
        OfflinePlayer owner = Bukkit.getOfflinePlayer(egg.getOwnerUuid());
        String ownerName = owner.getName() != null ? owner.getName() : "Неизвестно";

        double avgIncome = (config.getEggIncomeMinAmount() + config.getEggIncomeMaxAmount()) / 2.0;
        if (isEclipseActive) {
            avgIncome *= config.getEclipseIncomeMultiplier();
        }

        hologramManager.updatePlacedEggHologram(
                egg.getId(),
                loc,
                ownerName,
                egg.getCurrentDurability(),
                egg.getMaxDurability(),
                avgIncome,
                config.getEggIncomeCurrency()
        );
    }

    private void processAuraTick() {
        for (PlacedEgg egg : activeEggs.values()) {
            World world = Bukkit.getWorld(egg.getWorldName());
            if (world == null) continue;

            Location center = new Location(world, egg.getX() + 0.5, egg.getY() + 0.5, egg.getZ() + 0.5);
            double radius = config.getEggAuraRadius();
            double radiusSq = radius * radius;

            for (Player player : world.getPlayers()) {
                if (player.getLocation().distanceSquared(center) <= radiusSq) {
                    if (player.getUniqueId().equals(egg.getOwnerUuid())) {
                        applyEffects(player, config.getEggAuraOwnerEffects());
                    } else {
                        applyEffects(player, config.getEggAuraEnemyEffects());
                    }
                }
            }
        }
    }

    private void applyEffects(Player player, List<String> effectSpecs) {
        for (String spec : effectSpecs) {
            String[] parts = spec.split(":");
            if (parts.length < 2) continue;
            try {
                String name = parts[0].toLowerCase();
                PotionEffectType type = PotionEffectType.getByKey(org.bukkit.NamespacedKey.minecraft(name));
                if (type == null) {
                    type = PotionEffectType.getByName(parts[0].toUpperCase());
                }
                if (type != null) {
                    int amplifier = Math.max(0, Integer.parseInt(parts[1]) - 1);
                    player.addPotionEffect(new PotionEffect(type, 45, amplifier, false, false, true));
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void processCosmeticParticles() {
        particleAngle += Math.PI / 10;
        if (particleAngle >= 2 * Math.PI) particleAngle = 0;

        for (PlacedEgg egg : activeEggs.values()) {
            World world = Bukkit.getWorld(egg.getWorldName());
            if (world == null || !world.isChunkLoaded(egg.getX() >> 4, egg.getZ() >> 4)) continue;

            Location center = new Location(world, egg.getX() + 0.5, egg.getY() + 0.5, egg.getZ() + 0.5);
            String aura = egg.getAuraType().toUpperCase();

            double rad = 0.8;
            double ox = rad * Math.cos(particleAngle);
            double oz = rad * Math.sin(particleAngle);
            Location ring1 = center.clone().add(ox, 0.4, oz);
            Location ring2 = center.clone().add(-ox, 0.4, -oz);

            switch (aura) {
                case "FLAME" -> {
                    world.spawnParticle(Particle.FLAME, ring1, 2, 0.05, 0.05, 0.05, 0.02);
                    world.spawnParticle(Particle.LAVA, ring2, 1, 0.02, 0.02, 0.02, 0.01);
                }
                case "ELECTRIC" -> {
                    world.spawnParticle(Particle.ELECTRIC_SPARK, ring1, 3, 0.1, 0.1, 0.1, 0.05);
                    world.spawnParticle(Particle.CRIT, ring2, 2, 0.05, 0.05, 0.05, 0.02);
                }
                case "RUNES" -> {
                    world.spawnParticle(Particle.ENCHANT, center.clone().add(0, 0.9, 0), 4, 0.3, 0.3, 0.3, 0.05);
                    world.spawnParticle(Particle.SOUL, ring1, 1, 0.05, 0.05, 0.05, 0.01);
                }
                case "SOULS" -> {
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, ring1, 2, 0.05, 0.05, 0.05, 0.02);
                    world.spawnParticle(Particle.SOUL, ring2, 2, 0.05, 0.05, 0.05, 0.02);
                }
                default -> {
                    world.spawnParticle(Particle.PORTAL, ring1, 3, 0.1, 0.1, 0.1, 0.02);
                    world.spawnParticle(Particle.DRAGON_BREATH, ring2, 1, 0.05, 0.05, 0.05, 0.01);
                }
            }
        }
    }

    private void spawnSentinels(PlacedEgg egg) {
        if (!config.isSentinelsEnabled()) return;
        World world = Bukkit.getWorld(egg.getWorldName());
        if (world == null) return;

        cleanupSentinels(egg.getId());
        List<Entity> list = new ArrayList<>();

        EntityType type;
        try {
            type = EntityType.valueOf(config.getSentinelsType().toUpperCase());
        } catch (Exception e) {
            type = EntityType.ENDERMITE;
        }

        int count = config.getSentinelsCount();
        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI / count) * i;
            Location spawnLoc = new Location(world, egg.getX() + 0.5 + 1.5 * Math.cos(angle), egg.getY(), egg.getZ() + 0.5 + 1.5 * Math.sin(angle));
            Entity entity = world.spawnEntity(spawnLoc, type);
            entity.customName(Messages.colorize(config.getSentinelsName()));
            entity.setCustomNameVisible(true);
            entity.setGlowing(true);

            if (entity instanceof Creature creature) {
                AttributeInstance healthAttr = creature.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (healthAttr != null) {
                    healthAttr.setBaseValue(config.getSentinelsHealth());
                    creature.setHealth(config.getSentinelsHealth());
                }
            }
            list.add(entity);
        }
        sentinelsMap.put(egg.getId(), list);
    }

    private void cleanupSentinels(int eggId) {
        List<Entity> list = sentinelsMap.remove(eggId);
        if (list != null) {
            for (Entity e : list) {
                if (e != null && e.isValid()) {
                    e.remove();
                }
            }
        }
    }

    public void triggerRaidAlert(PlacedEgg egg, Player attacker) {
        long now = System.currentTimeMillis();
        long last = lastAlertTime.getOrDefault(egg.getId(), 0L);
        if (now - last < 30000L) {
            return;
        }
        lastAlertTime.put(egg.getId(), now);

        Player owner = Bukkit.getPlayer(egg.getOwnerUuid());
        if (owner != null && owner.isOnline()) {
            messages.send(owner, "placed-egg-attacked",
                    Messages.Placeholder.of("x", egg.getX()),
                    Messages.Placeholder.of("y", egg.getY()),
                    Messages.Placeholder.of("z", egg.getZ()),
                    Messages.Placeholder.of("player", attacker.getName())
            );
            owner.playSound(owner.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        }

        if (telegramNotifier.isEnabled()) {
            String text = "🚨 <b>ТРЕВОГА FrameEnd!</b>\n" +
                    "Ваше <b>Яйцо Дракона</b> атаковано игроком: <code>" + attacker.getName() + "</code>\n" +
                    "📍 Координаты: <code>X: " + egg.getX() + " Y: " + egg.getY() + " Z: " + egg.getZ() + "</code>\n" +
                    "🌍 Мир: <code>" + egg.getWorldName() + "</code>";
            telegramNotifier.sendAlert(text);
        }
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
            Player player = owner.getPlayer();

            double min = config.getEggIncomeMinAmount();
            double max = config.getEggIncomeMaxAmount();
            double baseIncome = min + (max - min) * random.nextDouble();
            double finalIncome = baseIncome;

            if (isEclipseActive) {
                finalIncome *= config.getEclipseIncomeMultiplier();
            }

            boolean inRange = true;
            if (player != null && player.isOnline()) {
                if (!player.getWorld().getName().equals(egg.getWorldName())) {
                    inRange = false;
                } else {
                    Location eggLoc = new Location(world, egg.getX() + 0.5, egg.getY() + 0.5, egg.getZ() + 0.5);
                    double dist = player.getLocation().distance(eggLoc);
                    double radius = config.getEggIncomeRadius();

                    if (radius > 0 && dist > radius) {
                        inRange = false;
                    } else if (config.isEggDistanceScalingEnabled() && radius > 0) {
                        double factor = Math.max(0.0, Math.min(1.0, 1.0 - (dist / radius)));
                        double multiplier = 1.0 + factor * (config.getEggMaxDistanceMultiplier() - 1.0);
                        finalIncome = baseIncome * multiplier;
                    }
                }
            } else {
                inRange = false;
            }

            finalIncome = Math.round(finalIncome * 10.0) / 10.0;

            if (inRange && player != null) {
                if ("POINTS".equalsIgnoreCase(config.getEggIncomeCurrency())) {
                    pointsHook.givePoints(egg.getOwnerUuid(), (int) Math.round(finalIncome));
                } else {
                    vaultHook.deposit(owner, finalIncome);
                }

                messages.send(player, "placed-egg-income-received",
                        Messages.Placeholder.of("amount", String.format("%.1f", finalIncome)),
                        Messages.Placeholder.of("durability", egg.getCurrentDurability()),
                        Messages.Placeholder.of("max_durability", egg.getMaxDurability())
                );
            }

            int wear = config.getEggDurabilityLossPerPayout();
            if (isEclipseActive) {
                wear = (int) Math.round(wear * config.getEclipseDurabilityMultiplier());
            }

            boolean broken = egg.reduceDurability(wear);
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
                cleanupSentinels(egg.getId());
                messages.broadcast("placed-egg-destroyed-wear");
            }
        }

        for (int id : destroyedEggIds) {
            activeEggs.remove(id);
            hologramManager.remove("placed_egg_" + id);
            storage.deletePlacedEgg(id);
            cleanupSentinels(id);
        }
    }

    public void onEggPlaced(Player player, Block block, ItemStack item) {
        int durability = itemFactory.getDurability(item, config.getEggMaxDurability());
        int maxDurability = itemFactory.getMaxDurability(item, config.getEggMaxDurability());
        int repairCount = itemFactory.getRepairCount(item);

        int id = storage.insertPlacedEgg(player.getUniqueId(), block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ(),
                durability, maxDurability, repairCount, System.currentTimeMillis(), "DEFAULT");

        if (id > 0) {
            PlacedEgg placedEgg = new PlacedEgg(id, player.getUniqueId(), block.getWorld().getName(),
                    block.getX(), block.getY(), block.getZ(),
                    durability, maxDurability, repairCount, System.currentTimeMillis(), "DEFAULT");
            activeEggs.put(id, placedEgg);
            updateEggHologram(placedEgg);
            spawnSentinels(placedEgg);

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
        cleanupSentinels(found.getId());
        storage.deletePlacedEgg(found.getId());

        Location center = block.getLocation().add(0.5, 0.5, 0.5);
        AnimationUtil.playPlacedEggBreakAnimation(center);

        ItemStack droppedEgg = itemFactory.createEggItem(found.getCurrentDurability(), found.getMaxDurability(), found.getRepairCount());
        block.getWorld().dropItemNaturally(center, droppedEgg);

        String breakerName = player != null ? player.getName() : "Окружение";
        messages.broadcast("placed-egg-broken",
                Messages.Placeholder.of("player", breakerName)
        );
        return true;
    }

    public void dismantleEgg(Player player, PlacedEgg egg) {
        activeEggs.remove(egg.getId());
        hologramManager.remove("placed_egg_" + egg.getId());
        cleanupSentinels(egg.getId());
        storage.deletePlacedEgg(egg.getId());

        World world = Bukkit.getWorld(egg.getWorldName());
        if (world != null) {
            Block block = world.getBlockAt(egg.getX(), egg.getY(), egg.getZ());
            if (block.getType() == Material.DRAGON_EGG) {
                block.setType(Material.AIR);
            }
            Location center = new Location(world, egg.getX() + 0.5, egg.getY() + 0.5, egg.getZ() + 0.5);
            AnimationUtil.playPlacedEggBreakAnimation(center);
        }

        ItemStack eggItem = itemFactory.createEggItem(egg.getCurrentDurability(), egg.getMaxDurability(), egg.getRepairCount());
        Map<Integer, ItemStack> left = player.getInventory().addItem(eggItem);
        if (!left.isEmpty() && world != null) {
            Location dropLoc = player.getLocation();
            for (ItemStack s : left.values()) {
                world.dropItemNaturally(dropLoc, s);
            }
        }

        messages.send(player, "placed-egg-dismantled");
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
