package pluginsfix.frameend.listener;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import pluginsfix.frameend.config.FrameEndConfig;
import pluginsfix.frameend.domain.EventState;
import pluginsfix.frameend.domain.PlacedEgg;
import pluginsfix.frameend.egg.DragonEggItemFactory;
import pluginsfix.frameend.egg.EggRepairMenu;
import pluginsfix.frameend.egg.PlacedEggManager;
import pluginsfix.frameend.event.EndWorldEventManager;
import pluginsfix.frameend.hook.PlayerPointsHook;
import pluginsfix.frameend.hook.VaultEconomyHook;
import pluginsfix.frameend.storage.Storage;
import pluginsfix.frameend.text.Messages;

import java.util.Optional;

public final class EggInteractionListener implements Listener {
    private final FrameEndConfig config;
    private final EndWorldEventManager eventManager;
    private final PlacedEggManager placedEggManager;
    private final DragonEggItemFactory eggItemFactory;
    private final Storage storage;
    private final Messages messages;
    private final VaultEconomyHook vaultHook;
    private final PlayerPointsHook pointsHook;

    public EggInteractionListener(FrameEndConfig config, EndWorldEventManager eventManager,
                                  PlacedEggManager placedEggManager, DragonEggItemFactory eggItemFactory,
                                  Storage storage, Messages messages,
                                  VaultEconomyHook vaultHook, PlayerPointsHook pointsHook) {
        this.config = config;
        this.eventManager = eventManager;
        this.placedEggManager = placedEggManager;
        this.eggItemFactory = eggItemFactory;
        this.storage = storage;
        this.messages = messages;
        this.vaultHook = vaultHook;
        this.pointsHook = pointsHook;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.DRAGON_EGG) return;

        if (eventManager.getState() == EventState.EGG_PHASE && eventManager.getEggPhase().isEventEgg(clicked.getLocation())) {
            event.setCancelled(true);
            if (event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                eventManager.getEggPhase().handleEggHit(event.getPlayer(), clicked);
            }
            return;
        }

        Optional<PlacedEgg> placedEggOpt = placedEggManager.getEggAt(clicked.getLocation());
        if (placedEggOpt.isPresent()) {
            event.setCancelled(true);
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Player player = event.getPlayer();
                EggRepairMenu menu = new EggRepairMenu(placedEggOpt.get(), config, storage, messages, vaultHook, pointsHook, placedEggManager);
                player.openInventory(menu.getInventory());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockDamage(BlockDamageEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.DRAGON_EGG) return;

        if (eventManager.getState() == EventState.EGG_PHASE && eventManager.getEggPhase().isEventEgg(block.getLocation())) {
            event.setCancelled(true);
            eventManager.getEggPhase().handleEggHit(event.getPlayer(), block);
            return;
        }

        Optional<PlacedEgg> placedEggOpt = placedEggManager.getEggAt(block.getLocation());
        if (placedEggOpt.isPresent()) {
            PlacedEgg placedEgg = placedEggOpt.get();
            if (!event.getPlayer().getUniqueId().equals(placedEgg.getOwnerUuid())) {
                placedEggManager.triggerRaidAlert(placedEgg, event.getPlayer());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (eggItemFactory.isCustomDragonEgg(item)) {
            World world = event.getBlockPlaced().getWorld();
            if (world.getEnvironment() != World.Environment.NORMAL || !world.getName().equalsIgnoreCase(config.getPlacedEggAllowedWorld())) {
                event.setCancelled(true);
                messages.send(event.getPlayer(), "egg-place-world-not-allowed",
                        Messages.Placeholder.of("world", config.getPlacedEggAllowedWorld())
                );
                return;
            }
            placedEggManager.onEggPlaced(event.getPlayer(), event.getBlockPlaced(), item);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (eventManager.getState() == EventState.EGG_PHASE) {
            if (eventManager.getEggPhase().isEventEgg(block.getLocation())) {
                event.setCancelled(true);
                eventManager.getEggPhase().handleEggHit(event.getPlayer(), block);
                return;
            }
            if (eventManager.getEggPhase().isEventEgg(block.getLocation().clone().add(0, 1, 0))) {
                event.setCancelled(true);
                return;
            }
        }

        if (block.getType() == Material.DRAGON_EGG) {
            Optional<PlacedEgg> placedEggOpt = placedEggManager.getEggAt(block.getLocation());
            if (placedEggOpt.isPresent()) {
                PlacedEgg placedEgg = placedEggOpt.get();
                if (!event.getPlayer().getUniqueId().equals(placedEgg.getOwnerUuid())) {
                    placedEggManager.triggerRaidAlert(placedEgg, event.getPlayer());
                }
                event.setCancelled(true);
                event.setDropItems(false);
                placedEggManager.onEggBroken(event.getPlayer(), block);
                block.setType(Material.AIR);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEggTeleport(BlockFromToEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG || event.getTo() == Material.DRAGON_EGG) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b : event.getBlocks()) {
            if (b.getType() == Material.DRAGON_EGG || placedEggManager.getEggAt(b.getLocation()).isPresent() || eventManager.getAnchorPhase().isAnchorBlock(b.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block b : event.getBlocks()) {
            if (b.getType() == Material.DRAGON_EGG || placedEggManager.getEggAt(b.getLocation()).isPresent() || eventManager.getAnchorPhase().isAnchorBlock(b.getLocation())) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> {
            if (b.getType() == Material.DRAGON_EGG) {
                if (eventManager.getState() == EventState.EGG_PHASE && eventManager.getEggPhase().isEventEgg(b.getLocation())) {
                    return true;
                }
                if (placedEggManager.getEggAt(b.getLocation()).isPresent()) {
                    placedEggManager.onEggBroken(null, b);
                    b.setType(Material.AIR);
                    return true;
                }
            }
            return false;
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        event.blockList().removeIf(b -> {
            if (b.getType() == Material.DRAGON_EGG) {
                if (eventManager.getState() == EventState.EGG_PHASE && eventManager.getEggPhase().isEventEgg(b.getLocation())) {
                    return true;
                }
                if (placedEggManager.getEggAt(b.getLocation()).isPresent()) {
                    placedEggManager.onEggBroken(null, b);
                    b.setType(Material.AIR);
                    return true;
                }
            }
            return false;
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof EggRepairMenu repairMenu) {
            event.setCancelled(true);
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
                if (event.getWhoClicked() instanceof Player player) {
                    repairMenu.handleClick(event.getSlot(), player);
                }
            }
            return;
        }

        if (event.getInventory().getHolder() instanceof pluginsfix.frameend.egg.EggAuraMenu auraMenu) {
            event.setCancelled(true);
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getInventory())) {
                if (event.getWhoClicked() instanceof Player player) {
                    auraMenu.handleClick(event.getSlot(), player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof EggRepairMenu || event.getInventory().getHolder() instanceof pluginsfix.frameend.egg.EggAuraMenu) {
            event.setCancelled(true);
        }
    }
}
