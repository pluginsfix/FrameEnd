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

public final class LootEditorMenu implements InventoryHolder {
    private final LootManager lootManager;
    private final String category;
    private final String categoryDisplayName;
    private final Inventory inventory;

    public LootEditorMenu(LootManager lootManager, String category, String categoryDisplayName) {
        this.lootManager = lootManager;
        this.category = category;
        this.categoryDisplayName = categoryDisplayName;
        this.inventory = Bukkit.createInventory(this, 54, Messages.colorize("&#FB8808▶ &fЛут: &#FFFF00" + categoryDisplayName));
        setupItems();
    }

    private void setupItems() {
        List<ItemStack> items = lootManager.getCategoryItems(category);
        for (int i = 0; i < Math.min(items.size(), 45); i++) {
            inventory.setItem(i, items.get(i));
        }

        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            if (i != 49) {
                inventory.setItem(i, filler);
            }
        }

        ItemStack saveBtn = createItem(Material.EMERALD_BLOCK,
                "&#FFFF00◆ &fСохранить лут",
                "&#FFFF00◆ &fСохранить все предметы выше в конфигурацию.",
                "",
                "&#FB8808▶ &fНажмите ЛКМ для сохранения"
        );
        inventory.setItem(49, saveBtn);
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

    public void save(Player player) {
        List<ItemStack> items = new ArrayList<>();
        for (int i = 0; i < 45; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack != null && !stack.getType().isAir()) {
                items.add(stack.clone());
            }
        }
        lootManager.saveCategory(category, items);
        player.sendMessage(Messages.colorize("&#FFFF00◆ &fЛут категории &#FFFF00" + categoryDisplayName + " &fуспешно сохранён! &8(&#FB8808" + items.size() + " &fпредметов&8)"));
    }

    public String getCategory() {
        return category;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
