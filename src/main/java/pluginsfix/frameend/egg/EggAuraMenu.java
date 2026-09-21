package pluginsfix.frameend.egg;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import pluginsfix.frameend.domain.PlacedEgg;
import pluginsfix.frameend.storage.Storage;
import pluginsfix.frameend.text.Messages;

import java.util.ArrayList;
import java.util.List;

public final class EggAuraMenu implements InventoryHolder {
    private final Inventory inventory;
    private final PlacedEgg placedEgg;
    private final Storage storage;
    private final Messages messages;
    private final PlacedEggManager placedEggManager;

    public EggAuraMenu(PlacedEgg placedEgg, Storage storage, Messages messages, PlacedEggManager placedEggManager) {
        this.placedEgg = placedEgg;
        this.storage = storage;
        this.messages = messages;
        this.placedEggManager = placedEggManager;
        this.inventory = Bukkit.createInventory(this, 27, Messages.colorize("&#FB8808▶ &fКосметические Ауры Яйца"));
        setupItems();
    }

    private void setupItems() {
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        String current = placedEgg.getAuraType();

        inventory.setItem(10, createAuraItem(Material.FIRE_CHARGE, "FLAME", "&#FB8808◆ &fОгненная Буря", "Частицы яростного пламени и магмы.", current.equalsIgnoreCase("FLAME")));
        inventory.setItem(11, createAuraItem(Material.AMETHYST_SHARD, "ELECTRIC", "&#FFFF00◆ &fЭлектрический Разряд", "Искры электричества и молний.", current.equalsIgnoreCase("ELECTRIC")));
        inventory.setItem(12, createAuraItem(Material.ENCHANTED_BOOK, "RUNES", "&#FFFF00◆ &fРуны Бездны", "Магические символы зачарования.", current.equalsIgnoreCase("RUNES")));
        inventory.setItem(13, createAuraItem(Material.ENDER_EYE, "PORTAL", "&#FB8808◆ &fПортальный Вихрь", "Эфирные частицы портала и дыхания.", current.equalsIgnoreCase("PORTAL") || current.equalsIgnoreCase("DEFAULT")));
        inventory.setItem(14, createAuraItem(Material.SOUL_LANTERN, "SOULS", "&#FFFF00◆ &fДуши Преисподней", "Лазурные огни душ из глубин.", current.equalsIgnoreCase("SOULS")));

        ItemStack backBtn = createItem(Material.ARROW, "&#FB8808▶ &fНазад в меню", "&#FFFF00◆ &fВернуться к управлению яйцом");
        inventory.setItem(22, backBtn);
    }

    private ItemStack createAuraItem(Material mat, String type, String name, String desc, boolean selected) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Messages.colorize(name));
            List<Component> lore = new ArrayList<>();
            lore.add(Messages.colorize("&#FFFF00◆ &f" + desc));
            lore.add(Component.empty());
            if (selected) {
                lore.add(Messages.colorize("&#FFFF00▶ &fСтатус: &#FFFF00[ВЫБРАНО]"));
            } else {
                lore.add(Messages.colorize("&#FB8808▶ &fНажмите ЛКМ, чтобы выбрать эту ауру"));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createItem(Material mat, String name, String... loreLines) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Messages.colorize(name));
            if (loreLines.length > 0) {
                List<Component> lore = new ArrayList<>();
                for (String l : loreLines) {
                    lore.add(Messages.colorize(l));
                }
                meta.lore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public void handleClick(int slot, Player player) {
        String newAura = null;
        if (slot == 10) newAura = "FLAME";
        else if (slot == 11) newAura = "ELECTRIC";
        else if (slot == 12) newAura = "RUNES";
        else if (slot == 13) newAura = "PORTAL";
        else if (slot == 14) newAura = "SOULS";
        else if (slot == 22) {
            player.closeInventory();
            return;
        }

        if (newAura != null) {
            placedEgg.setAuraType(newAura);
            storage.updatePlacedEggAura(placedEgg.getId(), newAura);
            messages.send(player, "egg-aura-selected", Messages.Placeholder.of("aura", newAura));
            setupItems();
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
