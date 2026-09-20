package pluginsfix.frameend.hook;

import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public final class PlayerPointsHook {
    private PlayerPointsAPI api;

    public PlayerPointsHook() {
        tryHook();
    }

    private void tryHook() {
        if (this.api != null) return;
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (plugin instanceof PlayerPoints pp) {
            this.api = pp.getAPI();
        }
    }

    public boolean isAvailable() {
        if (this.api == null) {
            tryHook();
        }
        return this.api != null;
    }

    public int getPoints(UUID playerUuid) {
        if (!isAvailable()) return 0;
        return api.look(playerUuid);
    }

    public boolean givePoints(UUID playerUuid, int points) {
        if (!isAvailable()) return false;
        return api.give(playerUuid, points);
    }

    public boolean takePoints(UUID playerUuid, int points) {
        if (!isAvailable()) return false;
        return api.take(playerUuid, points);
    }
}
