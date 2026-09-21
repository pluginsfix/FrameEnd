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
    private final PlacedEggManager placedEggManager;

    public EggRepairMenu(PlacedEgg placedEgg, FrameEndConfig config, Storage storage,
                          Messages messages, VaultEconomyHook vaultHook, PlayerPointsHook pointsHook,
                          PlacedEggManager placedEggManager) {
        this.placedEgg = placedEgg;
        this.config = config;
        this.storage = storage;
        this.messages = messages;
        this.vaultHook = vaultHook;
        this.pointsHook = pointsHook;
        this.placedEggManager = placedEggManager;
        this.inventory = Bukkit.createInventory(this, 27, Messages.colorize("&#FB8808▶ &fУправление Яйцом Дракона"));
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
        inventory.setItem(10, moneyBtn);

        ItemStack expBtn = createItem(Material.EXPERIENCE_BOTTLE,
                "&#FFFF00◆ &fПочинка за опыт",
                "&#FFFF00◆ &fВосстановить прочность до максимума",
                "",
                "&#FB8808▶ &fСтоимость: &#FFFF00" + expCost + " уровней",
                "&#FFFF00◆ &fНажмите ЛКМ для оплаты"
        );
        inventory.setItem(12, expBtn);

        ItemStack framesBtn = createItem(Material.NETHER_STAR,
                "&#FFFF00◆ &fПочинка за фреймы (донат)",
                "&#FFFF00◆ &fВосстановить прочность до максимума",
                "",
                "&#FB8808▶ &fСтоимость: &#FFFF00" + framesCost + " фреймов",
                "&#FFFF00◆ &fНажмите ЛКМ для оплаты"
        );
        inventory.setItem(14, framesBtn);

        ItemStack dismantleBtn = createItem(Material.BARRIER,
                "&#FB8808▶ &#FB8808Сломать и забрать яйцо",
                "&#FFFF00◆ &fДемонтировать яйцо дракона и вернуть",
                "&#FFFF00◆ &fего в ваш инвентарь.",
                "",
                "&#FB8808▶ &fНажмите ЛКМ для подтверждения"
        );
        inventory.setItem(16, dismantleBtn);

        ItemStack auraBtn = createItem(Material.NETHER_STAR,
                "&#FFFF00◆ &fКосметические Ауры",
                "&#FFFF00◆ &fВыбрать визуальные частицы и стиль яйца",
                "",
                "&#FB8808▶ &fНажмите ЛКМ для выбора ауры"
        );
        inventory.setItem(22, auraBtn);
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
        if (slot == 10 || slot == 12 || slot == 14) {
            if (placedEgg.getCurrentDurability() >= placedEgg.getMaxDurability()) {
                player.closeInventory();
                return;
            }
        }

        if (slot == 10) {
            double cost = placedEgg.calculateMoneyRepairCost(config.getBaseRepairCostMoney(), config.getRepairCostMultiplier());
            if (!vaultHook.has(player, cost)) {
                messages.send(player, "egg-repair-not-enough-resources");
                return;
            }
            if (vaultHook.withdraw(player, cost)) {
                performRepair(player);
            }
        } else if (slot == 12) {
            int expCost = placedEgg.calculateExpRepairCost(config.getBaseRepairCostExpLevels(), config.getRepairCostMultiplier());
            if (player.getLevel() < expCost) {
                messages.send(player, "egg-repair-not-enough-resources");
                return;
            }
            player.setLevel(player.getLevel() - expCost);
            performRepair(player);
        } else if (slot == 14) {
            int framesCost = placedEgg.calculateFramesRepairCost(config.getBaseRepairCostFrames(), config.getRepairCostMultiplier());
            if (pointsHook.getPoints(player.getUniqueId()) < framesCost) {
                messages.send(player, "egg-repair-not-enough-resources");
                return;
            }
            if (pointsHook.takePoints(player.getUniqueId(), framesCost)) {
                performRepair(player);
            }
        } else if (slot == 16) {
            if (!player.getUniqueId().equals(placedEgg.getOwnerUuid()) && !player.hasPermission("frameend.admin")) {
                messages.send(player, "no-permission");
                return;
            }
            player.closeInventory();
            if (placedEggManager != null) {
                placedEggManager.dismantleEgg(player, placedEgg);
            }
        } else if (slot == 22) {
            EggAuraMenu auraMenu = new EggAuraMenu(placedEgg, storage, messages, placedEggManager);
            player.openInventory(auraMenu.getInventory());
        }
    }

    private void performRepair(Player player) {
        placedEgg.repair(placedEgg.getMaxDurability());
        storage.updatePlacedEgg(placedEgg.getId(), placedEgg.getCurrentDurability(), placedEgg.getRepairCount());
        if (placedEggManager != null) {
            placedEggManager.updateEggAfterRepair(placedEgg);
        }
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
