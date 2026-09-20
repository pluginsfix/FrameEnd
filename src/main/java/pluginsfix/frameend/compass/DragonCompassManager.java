package pluginsfix.frameend.compass;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import pluginsfix.frameend.config.FrameEndConfig;
import pluginsfix.frameend.domain.PlacedEgg;
import pluginsfix.frameend.egg.PlacedEggManager;
import pluginsfix.frameend.storage.Storage;
import pluginsfix.frameend.text.Messages;

import java.util.Optional;

public final class DragonCompassManager {
    private final FrameEndConfig config;
    private final Storage storage;
    private final Messages messages;
    private final PlacedEggManager eggManager;

    public DragonCompassManager(FrameEndConfig config, Storage storage, Messages messages, PlacedEggManager eggManager) {
        this.config = config;
        this.storage = storage;
        this.messages = messages;
        this.eggManager = eggManager;
    }

    public void handleCompassUse(Player player, ItemStack item) {
        long now = System.currentTimeMillis();
        long cooldownUntil = storage.getCompassCooldown(player.getUniqueId());

        if (cooldownUntil > now) {
            long diffSeconds = (cooldownUntil - now) / 1000L;
            String formattedTime = formatTime(diffSeconds);
            messages.send(player, "compass-on-cooldown",
                    Messages.Placeholder.of("time", formattedTime)
            );
            return;
        }

        Optional<PlacedEgg> eggOpt = eggManager.findNearestOrFirstEgg(player.getLocation());
        if (eggOpt.isEmpty()) {
            messages.send(player, "compass-no-active-egg");
            return;
        }

        PlacedEgg egg = eggOpt.get();
        World targetWorld = Bukkit.getWorld(egg.getWorldName());
        if (targetWorld == null) {
            messages.send(player, "compass-no-active-egg");
            return;
        }

        Location targetLoc = new Location(targetWorld, egg.getX() + 0.5, egg.getY(), egg.getZ() + 0.5);

        if (item.getItemMeta() instanceof CompassMeta meta) {
            meta.setLodestoneTracked(false);
            meta.setLodestone(targetLoc);
            item.setItemMeta(meta);
        } else {
            player.setCompassTarget(targetLoc);
        }

        long nextCooldown = now + (config.getCompassCooldownSeconds() * 1000L);
        storage.setCompassCooldown(player.getUniqueId(), nextCooldown);

        messages.send(player, "compass-target-found",
                Messages.Placeholder.of("x", egg.getX()),
                Messages.Placeholder.of("y", egg.getY()),
                Messages.Placeholder.of("z", egg.getZ()),
                Messages.Placeholder.of("world", egg.getWorldName())
        );
    }

    private String formatTime(long totalSeconds) {
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append(" дн. ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append(" ч. ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append(" мин. ");
        }
        sb.append(seconds).append(" сек.");
        return sb.toString().trim();
    }
}
