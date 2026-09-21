package pluginsfix.frameend.listener;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import pluginsfix.frameend.config.FrameEndConfig;
import pluginsfix.frameend.event.EndWorldEventManager;
import pluginsfix.frameend.text.Messages;

public final class PortalAccessListener implements Listener {
    private final FrameEndConfig config;
    private final EndWorldEventManager eventManager;
    private final Messages messages;

    public PortalAccessListener(FrameEndConfig config, EndWorldEventManager eventManager, Messages messages) {
        this.config = config;
        this.eventManager = eventManager;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent event) {
        if (event.getTo() == null || event.getTo().getWorld() == null) return;

        World targetWorld = event.getTo().getWorld();
        if (targetWorld.getName().equals(config.getEndWorldName())) {
            if (!eventManager.isPortalOpen() && !event.getPlayer().hasPermission("frameend.bypass")) {
                event.setCancelled(true);
                messages.send(event.getPlayer(), "portal-closed",
                        Messages.Placeholder.of("time", eventManager.getFormattedTimeUntilNextEvent())
                );
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null || event.getTo().getWorld() == null) return;

        Player player = event.getPlayer();
        if (event.getTo().getWorld().getName().equals(config.getEndWorldName())) {
            if (!eventManager.isPortalOpen() && !player.hasPermission("frameend.bypass")) {
                event.setCancelled(true);
                messages.send(player, "portal-closed",
                        Messages.Placeholder.of("time", eventManager.getFormattedTimeUntilNextEvent())
                );
            }
        }
    }
}
