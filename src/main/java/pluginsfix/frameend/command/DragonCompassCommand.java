package pluginsfix.frameend.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import pluginsfix.frameend.compass.DragonCompassManager;
import pluginsfix.frameend.text.Messages;

import java.util.Collections;
import java.util.List;

public final class DragonCompassCommand implements CommandExecutor, TabCompleter {
    private final DragonCompassManager compassManager;
    private final Messages messages;

    public DragonCompassCommand(DragonCompassManager compassManager, Messages messages) {
        this.compassManager = compassManager;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            messages.send(sender, "player-only");
            return true;
        }

        if (!player.hasPermission("frameend.compass.use")) {
            messages.send(player, "no-permission");
            return true;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        compassManager.handleCompassUse(player, mainHand);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
