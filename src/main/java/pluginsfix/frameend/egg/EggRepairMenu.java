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
import pluginsfix.frameend.config.FrameEndConfig;
import pluginsfix.frameend.domain.PlacedEgg;
import pluginsfix.frameend.hook.PlayerPointsHook;
import pluginsfix.frameend.hook.VaultEconomyHook;
import pluginsfix.frameend.storage.Storage;
import pluginsfix.frameend.text.Messages;

import java.util.ArrayList;
import java.util.List;

public final class EggRepairMenu implements InventoryHolder {
    private final Inventory inventory;
    private final PlacedEgg placedEgg;
    private final FrameEndConfig config;
    private final Storage storage;
    private final Messages messages;
    private final VaultEconomyHook vaultHook;
    private final PlayerPointsHook pointsHook;

    public EggRepairMenu(PlacedEgg placedEgg, FrameEndConfig config, Storage storage,
                         Messages messages, VaultEconomyHook vaultHook, PlayerPointsHook pointsHook) {
        this.placedEgg = placedEgg;
        this.config = config;
        this.storage = storage;
        this.messages = messages;
        this.vaultHook = vaultHook;
        this.pointsHook = pointsHook;
        this.inventory = Bukkit.createInventory(this, 27, Messages.colorize("&#FB8808▶ &fПочинка Яйца Дракона"));
        setupItems();
    }

    private void setupItems() {
        ItemStack filler = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, filler);
        }

        double moneyCost = placedEgg.calculateMoneyRepairCost(config.getBaseRepairCostMoney(), config.getRepairCostMultiplier());
        int expCost = placedEgg.calculateExpRepairCost(config.getBaseRepairCostExpLevels(), config.getRepairCostMultiplier());
        int framesCost = placedEgg.calculateFramesRepairCost(config.getBaseRepairCostFrames(), config.getRepairCostMultiplier());

        ItemStack infoItem = new ItemStack(Material.DRAGON_EGG);
        ItemMeta infoMeta = infoItem.getItemMeta();
        if (infoMeta != null) {
            infoMeta.displayName(Messages.colorize("&#FFFF00◆ &fСостояние Яйца Дракона"));
            List<Component> lore = new ArrayList<>();
            lore.add(Messages.colorize("&#FFFF00◆ &fТекущий износ: &#FB8808" + placedEgg.getCurrentDurability() + "&8/&#FFFF00" + placedEgg.getMaxDurability()));
            lore.add(Messages.colorize("&#FFFF00◆ &fВсего починок: &#FFFF00" + placedEgg.getRepairCount()));
            lore.add(Component.empty());
            lore.add(Messages.colorize("&#FB8808▶ &fВыберите способ восстановления ниже:"));
            infoMeta.lore(lore);
            infoItem.setItemMeta(infoMeta);
        }
        inventory.setItem(4, infoItem);

        ItemStack moneyBtn = createItem(Material.GOLD_INGOT,
                "&#FFFF00◆ &fПочинка за монеты",
                "&#FFFF00◆ &fВосстановить прочность до максимума",
                "",
                "&#FB8808▶ &fСтоимость: &#FFFF00" + String.format("%.1f", moneyCost) + " монет",
                "&#FFFF00◆ &fНажмите ЛКМ для оплаты"
        );
        inventory.setItem(11, moneyBtn);

        ItemStack expBtn = createItem(Material.EXPERIENCE_BOTTLE,
                "&#FFFF00◆ &fПочинка за опыт",
                "&#FFFF00◆ &fВосстановить прочность до максимума",
                "",
                "&#FB8808▶ &fСтоимость: &#FFFF00" + expCost + " уровней",
                "&#FFFF00◆ &fНажмите ЛКМ для оплаты"
        );
        inventory.setItem(13, expBtn);

        ItemStack framesBtn = createItem(Material.NETHER_STAR,
                "&#FFFF00◆ &fПочинка за фреймы (донат)",
                "&#FFFF00◆ &fВосстановить прочность до максимума",
                "",
                "&#FB8808▶ &fСтоимость: &#FFFF00" + framesCost + " фреймов",
                "&#FFFF00◆ &fНажмите ЛКМ для оплаты"
        );
        inventory.setItem(15, framesBtn);
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
            double cost = placedEgg.calculateMoneyRepairCost(config.getBaseRepairCostMoney(), config.getRepairCostMultiplier());
            if (!vaultHook.has(player, cost)) {
                messages.send(player, "egg-repair-not-enough-resources");
                return;
            }
            if (vaultHook.withdraw(player, cost)) {
                performRepair(player);
            }
        } else if (slot == 13) {
            int expCost = placedEgg.calculateExpRepairCost(config.getBaseRepairCostExpLevels(), config.getRepairCostMultiplier());
            if (player.getLevel() < expCost) {
                messages.send(player, "egg-repair-not-enough-resources");
                return;
            }
            player.setLevel(player.getLevel() - expCost);
            performRepair(player);
        } else if (slot == 15) {
            int framesCost = placedEgg.calculateFramesRepairCost(config.getBaseRepairCostFrames(), config.getRepairCostMultiplier());
            if (pointsHook.getPoints(player.getUniqueId()) < framesCost) {
                messages.send(player, "egg-repair-not-enough-resources");
                return;
            }
            if (pointsHook.takePoints(player.getUniqueId(), framesCost)) {
                performRepair(player);
            }
        }
    }

    private void performRepair(Player player) {
        placedEgg.repair(placedEgg.getMaxDurability());
        storage.updatePlacedEgg(placedEgg.getId(), placedEgg.getCurrentDurability(), placedEgg.getRepairCount());
        messages.send(player, "egg-repair-success",
                Messages.Placeholder.of("repairs", placedEgg.getRepairCount())
        );
        player.closeInventory();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public PlacedEgg getPlacedEgg() {
        return placedEgg;
    }
}
