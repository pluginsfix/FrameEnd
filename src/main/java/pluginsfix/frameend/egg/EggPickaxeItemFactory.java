package pluginsfix.frameend.egg;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import pluginsfix.frameend.text.Messages;

import java.util.ArrayList;
import java.util.List;

public final class EggPickaxeItemFactory {
    private final NamespacedKey pickaxeKey;

    public EggPickaxeItemFactory(Plugin plugin) {
        this.pickaxeKey = new NamespacedKey(plugin, "egg_breaker_pickaxe");
    }

    public ItemStack createPickaxe() {
        ItemStack item = new ItemStack(Material.NETHERITE_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(Messages.colorize("&#FB8808▶ &#FFFF00Кирка Крушителя Энда"));

        List<Component> lore = new ArrayList<>();
        lore.add(Messages.colorize("&#FFFF00◆ &fЛегендарный инструмент для захвата реликвий."));
        lore.add(Messages.colorize("&#FFFF00◆ &fКрушит Яйцо Дракона и Якоря Края в мгновение ока!"));
        lore.add(Component.empty());
        lore.add(Messages.colorize("&#FB8808▶ &fУдар по яйцу: &#FFFF00+100 ударов &8(за 1 взмах)"));
        lore.add(Messages.colorize("&#FB8808▶ &fShift + Удар: &#FFFF00Мгновенный захват &8(+500 ударов)"));
        lore.add(Messages.colorize("&#FB8808▶ &fУдар по якорям: &#FFFF00Мгновенный раскол"));
        lore.add(Component.empty());
        lore.add(Messages.colorize("&#FFFF00◆ &fРедкость: &#FB8808Божественная &8(Инструмент проверки)"));
        meta.lore(lore);

        meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(pickaxeKey, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isEggBreakerPickaxe(ItemStack item) {
        if (item == null || item.getType() != Material.NETHERITE_PICKAXE || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(pickaxeKey, PersistentDataType.BYTE);
    }
}
