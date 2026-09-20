package pluginsfix.frameend.listener;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;
import pluginsfix.frameend.config.FrameEndConfig;
import pluginsfix.frameend.event.EndWorldEventManager;
import pluginsfix.frameend.text.Messages;

public final class IslandBoundaryListener implements Listener {
    private final FrameEndConfig config;
    private final EndWorldEventManager eventManager;
    private final Messages messages;

    public IslandBoundaryListener(FrameEndConfig config, EndWorldEventManager eventManager, Messages messages) {
        this.config = config;
        this.eventManager = eventManager;
        this.messages = messages;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        Player player = event.getPlayer();
        World world = player.getWorld();
        if (!world.getName().equals(config.getEndWorldName())) return;
        if (!eventManager.isPortalOpen() || player.hasPermission("frameend.bypass")) return;

        Location loc = event.getTo();
        double distFromCenter = Math.hypot(loc.getX(), loc.getZ());
        if (distFromCenter > config.getEndIslandRadiusLimit()) {
            Vector pushBack = new Vector(-loc.getX(), 0.2, -loc.getZ()).normalize().multiply(1.5);
            player.setVelocity(pushBack);
            messages.send(player, "island-boundary-prevented");
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGatewayTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.END_GATEWAY) return;
        World world = event.getFrom().getWorld();
        if (world != null && world.getName().equals(config.getEndWorldName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGatewayPortal(PlayerPortalEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.END_GATEWAY) return;
        World world = event.getFrom().getWorld();
        if (world != null && world.getName().equals(config.getEndWorldName())) {
            event.setCancelled(true);
        }
    }
}
