package pluginsfix.frameend.loot;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import pluginsfix.frameend.text.Messages;

import java.util.ArrayList;
import java.util.List;

public final class LootCategoryMenu implements InventoryHolder {
    private final LootManager lootManager;
    private final Inventory inventory;

    public LootCategoryMenu(LootManager lootManager) {
        this.lootManager = lootManager;
        this.inventory = Bukkit.createInventory(this, 27, Messages.colorize("&#FB8808▶ &fРедактор Лута &8| &#FFFF00Категории"));
        setupItems();
    }

    private void setupItems() {
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        ItemStack commonBtn = createItem(Material.RESPAWN_ANCHOR,
                "&#FFFF00◆ &fЛут: &#FFFF00Якоря Возрождения",
                "&#FFFF00◆ &fНажмите для редактирования наград",
                "&#FFFF00◆ &fс обычных якорей возрождения.",
                "",
                "&#FB8808▶ &fНажмите ЛКМ для открытия"
        );
        inventory.setItem(11, commonBtn);

        ItemStack secretBtn = createItem(Material.CRYING_OBSIDIAN,
                "&#FB8808▶ &fЛут: &#FB8808Секретный Обелиск",
                "&#FFFF00◆ &fНажмите для редактирования наград",
                "&#FFFF00◆ &fс редких секретных обелисков.",
                "",
                "&#FB8808▶ &fНажмите ЛКМ для открытия"
        );
        inventory.setItem(13, secretBtn);

        ItemStack dragonBtn = createItem(Material.DRAGON_HEAD,
                "&#FB8808▶ &fЛут: &#FB8808Древний Эндер-Дракон",
                "&#FFFF00◆ &fНажмите для редактирования предметов,",
                "&#FFFF00◆ &fвыпадающих при убийстве дракона.",
                "",
                "&#FB8808▶ &fНажмите ЛКМ для открытия"
        );
        inventory.setItem(15, dragonBtn);
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
        if (slot == 11) {
            LootEditorMenu menu = new LootEditorMenu(lootManager, LootManager.CATEGORY_COMMON, "Обычные Якоря");
            player.openInventory(menu.getInventory());
        } else if (slot == 13) {
            LootEditorMenu menu = new LootEditorMenu(lootManager, LootManager.CATEGORY_SECRET, "Секретный Обелиск");
            player.openInventory(menu.getInventory());
        } else if (slot == 15) {
            LootEditorMenu menu = new LootEditorMenu(lootManager, LootManager.CATEGORY_DRAGON, "Древний Дракон");
            player.openInventory(menu.getInventory());
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
