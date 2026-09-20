package pluginsfix.frameend.compass;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import pluginsfix.frameend.config.FrameEndConfig;
import pluginsfix.frameend.text.Messages;

import java.util.ArrayList;
import java.util.List;

public final class DragonCompassItemFactory {
    private final NamespacedKey compassKey;
    private final FrameEndConfig config;

    public DragonCompassItemFactory(Plugin plugin, FrameEndConfig config) {
        this.compassKey = new NamespacedKey(plugin, "dragon_compass");
        this.config = config;
    }

    public ItemStack createCompass() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Messages.colorize(config.getCompassItemName()));

        List<Component> lore = new ArrayList<>();
        for (String line : config.getCompassItemLore()) {
            lore.add(Messages.colorize(line));
        }
        meta.lore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(compassKey, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isDragonCompass(ItemStack item) {
        if (item == null || item.getType() != Material.COMPASS || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(compassKey, PersistentDataType.BYTE);
    }
}
