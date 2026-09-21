package pluginsfix.frameend.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import pluginsfix.frameend.config.FrameEndConfig;
import pluginsfix.frameend.domain.EventState;
import pluginsfix.frameend.egg.DragonEggItemFactory;
import pluginsfix.frameend.hook.PlayerPointsHook;
import pluginsfix.frameend.loot.LootManager;
import pluginsfix.frameend.text.Messages;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Set;

public final class EndWorldEventManager {
    private final JavaPlugin plugin;
    private final FrameEndConfig config;
    private final Messages messages;
    private final DragonEggItemFactory eggItemFactory;
    private final pluginsfix.frameend.egg.EggPickaxeItemFactory pickaxeFactory;
    private final pluginsfix.frameend.hologram.HologramManager hologramManager;
    private final LootManager lootManager;
    private final PlayerPointsHook pointsHook;

    private EventState state = EventState.IDLE;
    private EndAnchorPhase anchorPhase;
    private DragonFightPhase dragonPhase;
    private EggCapturePhase eggPhase;

    private BukkitTask scheduleTask;
    private BukkitTask closingTask;
    private int closingRemainingSeconds;
    private final Set<Long> broadcastedIntervals = new HashSet<>();

    public EndWorldEventManager(JavaPlugin plugin, FrameEndConfig config, Messages messages,
                                DragonEggItemFactory eggItemFactory, pluginsfix.frameend.egg.EggPickaxeItemFactory pickaxeFactory,
                                pluginsfix.frameend.hologram.HologramManager hologramManager,
                                LootManager lootManager, PlayerPointsHook pointsHook) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.eggItemFactory = eggItemFactory;
        this.pickaxeFactory = pickaxeFactory;
        this.hologramManager = hologramManager;
        this.lootManager = lootManager;
        this.pointsHook = pointsHook;
    }

    public void init() {
        this.anchorPhase = new EndAnchorPhase(plugin, config, messages, hologramManager, lootManager, pointsHook, pickaxeFactory, this::advanceToDragonPhase);
        this.dragonPhase = new DragonFightPhase(plugin, config, messages, lootManager, this::advanceToEggPhase);
        this.eggPhase = new EggCapturePhase(plugin, config, messages, hologramManager, eggItemFactory, pickaxeFactory, this::advanceToClosingPhase);

        startScheduleChecker();
    }

    private void startScheduleChecker() {
        scheduleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkScheduleTick, 20L, 20L);
    }

    private void checkScheduleTick() {
        if (state != EventState.IDLE) return;

        ZonedDateTime now = ZonedDateTime.now(config.getTimezone());
        ZonedDateTime nextEvent = calculateNextEventTime(now);
        long secondsUntil = Duration.between(now, nextEvent).getSeconds();

        for (long interval : config.getBroadcastIntervals()) {
            if (secondsUntil == interval && !broadcastedIntervals.contains(interval)) {
                broadcastedIntervals.add(interval);
                messages.broadcast("event-broadcast-timer",
                        Messages.Placeholder.of("time", formatDuration(secondsUntil))
                );
            }
        }

        if (secondsUntil <= 0) {
            broadcastedIntervals.clear();
            startEvent();
        }
    }

    public void startEvent() {
        if (state != EventState.IDLE) {
            stopEvent();
        }

        state = EventState.ANCHORS_PHASE;
        messages.broadcast("event-started");
        anchorPhase.start();
    }

    public void advanceToDragonPhase() {
        if (state != EventState.ANCHORS_PHASE) return;
        state = EventState.DRAGON_PHASE;
        dragonPhase.start();
    }

    public void advanceToEggPhase() {
        if (state != EventState.DRAGON_PHASE) return;
        state = EventState.EGG_PHASE;
        eggPhase.start();
    }

    public void advanceToClosingPhase() {
        state = EventState.CLOSING_COUNTDOWN;
        closingRemainingSeconds = config.getCloseDelaySeconds();

        if (closingTask != null) {
            closingTask.cancel();
        }

        closingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (closingRemainingSeconds == 120 || closingRemainingSeconds == 60 ||
                    closingRemainingSeconds == 30 || closingRemainingSeconds == 10 || closingRemainingSeconds == 5) {
                messages.broadcast("event-closing-timer",
                        Messages.Placeholder.of("time", formatDuration(closingRemainingSeconds))
                );
            }

            closingRemainingSeconds--;
            if (closingRemainingSeconds <= 0) {
                if (closingTask != null) {
                    closingTask.cancel();
                    closingTask = null;
                }
                finishAndCloseEvent();
            }
        }, 20L, 20L);
    }

    public void finishAndCloseEvent() {
        teleportPlayersToSpawn();
        stopEvent();
        messages.broadcast("event-closed");
    }

    public void stopEvent() {
        state = EventState.IDLE;
        if (closingTask != null) {
            closingTask.cancel();
            closingTask = null;
        }
        if (anchorPhase != null) anchorPhase.end();
        if (dragonPhase != null) dragonPhase.cleanup();
        if (eggPhase != null) eggPhase.cleanup();
    }

    public void nextPhase() {
        switch (state) {
            case IDLE -> startEvent();
            case ANCHORS_PHASE -> {
                anchorPhase.end();
                advanceToDragonPhase();
            }
            case DRAGON_PHASE -> {
                World end = Bukkit.getWorld(config.getEndWorldName());
                Location loc = end != null ? new Location(end, 0, 70, 0) : new Location(Bukkit.getWorlds().get(0), 0, 70, 0);
                dragonPhase.onDragonDeath(loc);
            }
            case EGG_PHASE -> {
                eggPhase.cleanup();
                advanceToClosingPhase();
            }
            case CLOSING_COUNTDOWN -> finishAndCloseEvent();
            default -> {}
        }
    }

    public void teleportPlayersToSpawn() {
        World endWorld = Bukkit.getWorld(config.getEndWorldName());
        World spawnWorld = Bukkit.getWorld(config.getSpawnWorldName());
        if (endWorld == null || spawnWorld == null) return;

        Location spawnLoc = new Location(spawnWorld, config.getSpawnX(), config.getSpawnY(), config.getSpawnZ(), config.getSpawnYaw(), config.getSpawnPitch());
        for (Player player : endWorld.getPlayers()) {
            player.teleport(spawnLoc);
        }
    }

    public boolean isPortalOpen() {
        return state != EventState.IDLE;
    }

    public Location getEventLocation() {
        World world = Bukkit.getWorld(config.getEndWorldName());
        if (world == null) return null;
        return new Location(world, 0.5, 70.0, 0.5);
    }

    public ZonedDateTime calculateNextEventTime(ZonedDateTime from) {
        ZonedDateTime closest = null;
        for (DayOfWeek day : config.getScheduleDays()) {
            ZonedDateTime candidate = from.with(TemporalAdjusters.nextOrSame(day))
                    .withHour(config.getScheduleTime().getHour())
                    .withMinute(config.getScheduleTime().getMinute())
                    .withSecond(0)
                    .withNano(0);

            if (candidate.isBefore(from) || candidate.isEqual(from)) {
                candidate = candidate.plusWeeks(1);
            }

            if (closest == null || candidate.isBefore(closest)) {
                closest = candidate;
            }
        }
        return closest != null ? closest : from.plusDays(1);
    }

    public String getFormattedTimeUntilNextEvent() {
        ZonedDateTime now = ZonedDateTime.now(config.getTimezone());
        ZonedDateTime next = calculateNextEventTime(now);
        long seconds = Duration.between(now, next).getSeconds();
        return formatDuration(seconds);
    }

    public String formatDuration(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(" дн. ");
        if (hours > 0 || days > 0) sb.append(hours).append(" ч. ");
        if (minutes > 0 || hours > 0 || days > 0) sb.append(minutes).append(" мин. ");
        sb.append(seconds).append(" сек.");
        return sb.toString().trim();
    }

    public void shutdown() {
        if (scheduleTask != null) {
            scheduleTask.cancel();
            scheduleTask = null;
        }
        stopEvent();
    }

    public EventState getState() { return state; }
    public EndAnchorPhase getAnchorPhase() { return anchorPhase; }
    public DragonFightPhase getDragonPhase() { return dragonPhase; }
    public EggCapturePhase getEggPhase() { return eggPhase; }
}
