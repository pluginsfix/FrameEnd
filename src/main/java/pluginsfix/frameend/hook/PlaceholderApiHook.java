package pluginsfix.frameend.hook;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import pluginsfix.frameend.domain.EventState;
import pluginsfix.frameend.egg.PlacedEggManager;
import pluginsfix.frameend.event.EndWorldEventManager;
import pluginsfix.frameend.storage.Storage;

public final class PlaceholderApiHook extends PlaceholderExpansion {
    private final EndWorldEventManager eventManager;
    private final PlacedEggManager eggManager;
    private final Storage storage;

    public PlaceholderApiHook(EndWorldEventManager eventManager, PlacedEggManager eggManager, Storage storage) {
        this.eventManager = eventManager;
        this.eggManager = eggManager;
        this.storage = storage;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "frameend";
    }

    @Override
    public @NotNull String getAuthor() {
        return "pluginsfix";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if ("status".equalsIgnoreCase(params)) {
            return eventManager.isPortalOpen() ? "Активен" : "Не активен";
        }
        if ("phase".equalsIgnoreCase(params)) {
            return switch (eventManager.getState()) {
                case IDLE -> "Ожидание";
                case STARTING_COUNTDOWN -> "Подготовка";
                case ANCHORS_PHASE -> "Якоря возрождения";
                case DRAGON_PHASE -> "Битва с Драконом";
                case EGG_PHASE -> "Охота за Яйцом";
                case CLOSING_COUNTDOWN -> "Закрытие";
            };
        }
        if ("next_event".equalsIgnoreCase(params)) {
            return eventManager.getFormattedTimeUntilNextEvent();
        }
        if ("portal_open".equalsIgnoreCase(params)) {
            return String.valueOf(eventManager.isPortalOpen());
        }
        if ("placed_eggs_count".equalsIgnoreCase(params)) {
            return String.valueOf(eggManager.getActiveEggs().size());
        }
        if ("compass_cooldown".equalsIgnoreCase(params) && player != null) {
            long cooldownUntil = storage.getCompassCooldown(player.getUniqueId());
            long diff = (cooldownUntil - System.currentTimeMillis()) / 1000L;
            if (diff <= 0) return "Готов";
            return eventManager.formatDuration(diff);
        }
        return null;
    }
}
