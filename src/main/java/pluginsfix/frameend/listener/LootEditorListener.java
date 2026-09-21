package pluginsfix.frameend.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import pluginsfix.frameend.loot.LootCategoryMenu;
import pluginsfix.frameend.loot.LootEditorMenu;

public final class LootEditorListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof LootCategoryMenu categoryMenu) {
            event.setCancelled(true);
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(inv)) {
                if (event.getWhoClicked() instanceof Player player) {
                    categoryMenu.handleClick(event.getSlot(), player);
                }
            }
            return;
        }

        if (inv.getHolder() instanceof LootEditorMenu editorMenu) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(inv)) {
                int slot = event.getSlot();
                if (slot >= 45 && slot < 54) {
                    event.setCancelled(true);
                    if (slot == 49 && event.getWhoClicked() instanceof Player player) {
                        editorMenu.save(player);
                        player.closeInventory();
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        if (inv.getHolder() instanceof LootEditorMenu editorMenu) {
            if (event.getPlayer() instanceof Player player) {
                editorMenu.save(player);
            }
        }
    }
}
