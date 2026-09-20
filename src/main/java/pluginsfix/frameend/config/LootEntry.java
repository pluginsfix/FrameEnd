package pluginsfix.frameend.config;

import org.bukkit.Material;

public record LootEntry(
        Material material,
        int amountMin,
        int amountMax,
        double chance,
        String name,
        String command
) {
    public boolean hasCommand() {
        return command != null && !command.isBlank();
    }

    public boolean hasMaterial() {
        return material != null;
    }
}
