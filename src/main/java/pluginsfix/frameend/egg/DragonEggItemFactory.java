package pluginsfix.frameend.egg;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import pluginsfix.frameend.text.Messages;

import java.util.ArrayList;
import java.util.List;

public final class DragonEggItemFactory {
    private final NamespacedKey customEggKey;
    private final NamespacedKey durabilityKey;
    private final NamespacedKey maxDurabilityKey;
    private final NamespacedKey repairCountKey;

    public DragonEggItemFactory(Plugin plugin) {
        this.customEggKey = new NamespacedKey(plugin, "custom_egg");
        this.durabilityKey = new NamespacedKey(plugin, "durability");
        this.maxDurabilityKey = new NamespacedKey(plugin, "max_durability");
        this.repairCountKey = new NamespacedKey(plugin, "repair_count");
    }

    public ItemStack createEggItem(int durability, int maxDurability, int repairCount) {
        ItemStack item = new ItemStack(Material.DRAGON_EGG);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Messages.colorize("&#FFFF00◆ &fЯйцо Древнего Дракона"));

        List<Component> lore = new ArrayList<>();
        lore.add(Messages.colorize("&#FFFF00◆ &fРеликтовый артефакт павшего дракона."));
        lore.add(Messages.colorize("&#FFFF00◆ &fПриносит пассивный доход при установке."));
        lore.add(Component.empty());
        lore.add(Messages.colorize("&#FB8808▶ &fПрочность: &#FFFF00" + durability + "&8/&#FFFF00" + maxDurability));
        lore.add(Messages.colorize("&#FB8808▶ &fПочинок выполнено: &#FFFF00" + repairCount));
        meta.lore(lore);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(customEggKey, PersistentDataType.BYTE, (byte) 1);
        pdc.set(durabilityKey, PersistentDataType.INTEGER, durability);
        pdc.set(maxDurabilityKey, PersistentDataType.INTEGER, maxDurability);
        pdc.set(repairCountKey, PersistentDataType.INTEGER, repairCount);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isCustomDragonEgg(ItemStack item) {
        if (item == null || item.getType() != Material.DRAGON_EGG || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(customEggKey, PersistentDataType.BYTE);
    }

    public int getDurability(ItemStack item, int defaultVal) {
        if (!isCustomDragonEgg(item)) return defaultVal;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.getOrDefault(durabilityKey, PersistentDataType.INTEGER, defaultVal);
    }

    public int getMaxDurability(ItemStack item, int defaultVal) {
        if (!isCustomDragonEgg(item)) return defaultVal;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.getOrDefault(maxDurabilityKey, PersistentDataType.INTEGER, defaultVal);
    }

    public int getRepairCount(ItemStack item) {
        if (!isCustomDragonEgg(item)) return 0;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.getOrDefault(repairCountKey, PersistentDataType.INTEGER, 0);
    }
}
