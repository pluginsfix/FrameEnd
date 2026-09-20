package pluginsfix.frameend.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import pluginsfix.frameend.compass.DragonCompassItemFactory;
import pluginsfix.frameend.compass.DragonCompassManager;

public final class CompassUseListener implements Listener {
    private final DragonCompassItemFactory compassItemFactory;
    private final DragonCompassManager compassManager;

    public CompassUseListener(DragonCompassItemFactory compassItemFactory, DragonCompassManager compassManager) {
        this.compassItemFactory = compassItemFactory;
        this.compassManager = compassManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (item == null || !compassItemFactory.isDragonCompass(item)) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        compassManager.handleCompassUse(player, item);
    }
}
