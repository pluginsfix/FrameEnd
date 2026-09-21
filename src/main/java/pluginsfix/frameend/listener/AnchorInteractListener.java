package pluginsfix.frameend.listener;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import pluginsfix.frameend.domain.EventState;
import pluginsfix.frameend.event.EndWorldEventManager;

public final class AnchorInteractListener implements Listener {
    private final EndWorldEventManager eventManager;

    public AnchorInteractListener(EndWorldEventManager eventManager) {
        this.eventManager = eventManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (eventManager.getState() != EventState.ANCHORS_PHASE) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        if (eventManager.getAnchorPhase().isAnchorBlock(clicked.getLocation())) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.sendBlockChange(clicked.getLocation(), clicked.getBlockData());
            eventManager.getAnchorPhase().handleInteract(player, clicked);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(BlockDamageEvent event) {
        if (eventManager.getState() != EventState.ANCHORS_PHASE) return;

        Block block = event.getBlock();
        if (eventManager.getAnchorPhase().isAnchorBlock(block.getLocation())) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.sendBlockChange(block.getLocation(), block.getBlockData());
            eventManager.getAnchorPhase().handleInteract(player, block);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (eventManager.getState() != EventState.ANCHORS_PHASE) return;

        Block block = event.getBlock();
        if (eventManager.getAnchorPhase().isAnchorBlock(block.getLocation())) {
            event.setCancelled(true);
            Player player = event.getPlayer();
            player.sendBlockChange(block.getLocation(), block.getBlockData());
            eventManager.getAnchorPhase().handleInteract(player, block);
        }
    }
}
