package pluginsfix.frameend.command;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import pluginsfix.frameend.compass.DragonCompassItemFactory;
import pluginsfix.frameend.config.FrameEndConfig;
import pluginsfix.frameend.egg.DragonEggItemFactory;
import pluginsfix.frameend.event.EndWorldEventManager;
import pluginsfix.frameend.loot.LootCategoryMenu;
import pluginsfix.frameend.loot.LootManager;
import pluginsfix.frameend.text.Messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FrameEndCommand implements CommandExecutor, TabCompleter {
    private final FrameEndConfig config;
    private final Messages messages;
    private final EndWorldEventManager eventManager;
    private final DragonEggItemFactory eggItemFactory;
    private final DragonCompassItemFactory compassItemFactory;
    private final pluginsfix.frameend.egg.EggPickaxeItemFactory pickaxeItemFactory;
    private final LootManager lootManager;

    public FrameEndCommand(FrameEndConfig config, Messages messages, EndWorldEventManager eventManager,
                           DragonEggItemFactory eggItemFactory, DragonCompassItemFactory compassItemFactory,
                           pluginsfix.frameend.egg.EggPickaxeItemFactory pickaxeItemFactory,
                           LootManager lootManager) {
        this.config = config;
        this.messages = messages;
        this.eventManager = eventManager;
        this.eggItemFactory = eggItemFactory;
        this.compassItemFactory = compassItemFactory;
        this.pickaxeItemFactory = pickaxeItemFactory;
        this.lootManager = lootManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("frameend.admin")) {
            messages.send(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            messages.send(sender, "command-usage");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "start" -> {
                eventManager.startEvent();
                return true;
            }
            case "stop" -> {
                eventManager.finishAndCloseEvent();
                return true;
            }
            case "next" -> {
                eventManager.nextPhase();
                return true;
            }
            case "tp", "teleport" -> {
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "player-only");
                    return true;
                }
                Location loc = eventManager.getEventLocation();
                if (loc != null) {
                    player.teleport(loc);
                    messages.send(player, "event-teleported");
                }
                return true;
            }
            case "loot", "editloot" -> {
                if (!(sender instanceof Player player)) {
                    messages.send(sender, "player-only");
                    return true;
                }
                LootCategoryMenu menu = new LootCategoryMenu(lootManager);
                player.openInventory(menu.getInventory());
                return true;
            }
            case "giveegg" -> {
                if (args.length < 2) {
                    messages.send(sender, "command-invalid-args");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    messages.send(sender, "player-not-found", Messages.Placeholder.of("player", args[1]));
                    return true;
                }
                ItemStack egg = eggItemFactory.createEggItem(config.getEggMaxDurability(), config.getEggMaxDurability(), 0);
                target.getInventory().addItem(egg);
                messages.send(sender, "egg-given", Messages.Placeholder.of("player", target.getName()));
                return true;
            }
            case "givecompass" -> {
                if (args.length < 2) {
                    messages.send(sender, "command-invalid-args");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    messages.send(sender, "player-not-found", Messages.Placeholder.of("player", args[1]));
                    return true;
                }
                ItemStack compass = compassItemFactory.createCompass();
                target.getInventory().addItem(compass);
                messages.send(sender, "compass-given", Messages.Placeholder.of("player", target.getName()));
                return true;
            }
            case "givepickaxe", "pickaxe" -> {
                Player target;
                if (args.length >= 2) {
                    target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        messages.send(sender, "player-not-found", Messages.Placeholder.of("player", args[1]));
                        return true;
                    }
                } else if (sender instanceof Player player) {
                    target = player;
                } else {
                    messages.send(sender, "command-invalid-args");
                    return true;
                }
                ItemStack pickaxe = pickaxeItemFactory.createPickaxe();
                target.getInventory().addItem(pickaxe);
                messages.send(sender, "pickaxe-given", Messages.Placeholder.of("player", target.getName()));
                return true;
            }
            case "reload" -> {
                config.load();
                messages.load();
                if (lootManager != null) {
                    lootManager.load();
                }
                messages.send(sender, "config-reloaded");
                return true;
            }
            default -> {
                messages.send(sender, "command-usage");
                return true;
            }
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("frameend.admin")) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subCommands = List.of("start", "stop", "next", "tp", "editloot", "giveegg", "givecompass", "givepickaxe", "pickaxe", "reload");
            List<String> result = new ArrayList<>();
            for (String s : subCommands) {
                if (s.startsWith(args[0].toLowerCase())) {
                    result.add(s);
                }
            }
            return result;
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("giveegg") || args[0].equalsIgnoreCase("givecompass") || args[0].equalsIgnoreCase("givepickaxe") || args[0].equalsIgnoreCase("pickaxe"))) {
            List<String> players = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                    players.add(p.getName());
                }
            }
            return players;
        }

        return Collections.emptyList();
    }
}
