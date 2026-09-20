package pluginsfix.frameend.hologram;

import org.bukkit.Location;

import java.util.List;

public interface HologramService {
    void spawnOrUpdate(String id, Location location, List<String> lines);
    void remove(String id);
    void clearAll();
}
