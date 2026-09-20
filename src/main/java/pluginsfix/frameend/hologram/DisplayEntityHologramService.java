package pluginsfix.frameend.hologram;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;
import pluginsfix.frameend.text.Messages;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DisplayEntityHologramService implements HologramService {
    private final Map<String, UUID> spawnedEntities = new ConcurrentHashMap<>();

    @Override
    public void spawnOrUpdate(String id, Location location, List<String> lines) {
        World world = location.getWorld();
        if (world == null) return;

        Component textComponent = buildComponent(lines);
        UUID existingUuid = spawnedEntities.get(id);

        if (existingUuid != null) {
            Entity entity = world.getEntity(existingUuid);
            if (entity instanceof TextDisplay display && display.isValid()) {
                display.text(textComponent);
                display.teleport(location.clone().add(0, 0.4, 0));
                return;
            }
        }

        Location spawnLoc = location.clone().add(0, 0.4, 0);
        TextDisplay textDisplay = (TextDisplay) world.spawnEntity(spawnLoc, EntityType.TEXT_DISPLAY);
        textDisplay.text(textComponent);
        textDisplay.setBillboard(Display.Billboard.CENTER);
        textDisplay.setSeeThrough(false);
        textDisplay.setShadowed(true);
        textDisplay.setDefaultBackground(false);

        spawnedEntities.put(id, textDisplay.getUniqueId());
    }

    @Override
    public void remove(String id) {
        UUID uuid = spawnedEntities.remove(id);
        if (uuid == null) return;

        for (World world : org.bukkit.Bukkit.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity instanceof TextDisplay display) {
                display.remove();
            }
        }
    }

    @Override
    public void clearAll() {
        for (UUID uuid : spawnedEntities.values()) {
            for (World world : org.bukkit.Bukkit.getWorlds()) {
                Entity entity = world.getEntity(uuid);
                if (entity instanceof TextDisplay display) {
                    display.remove();
                }
            }
        }
        spawnedEntities.clear();
    }

    private Component buildComponent(List<String> lines) {
        Component result = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            Component lineComp = Messages.colorize(lines.get(i));
            result = result.append(lineComp);
            if (i < lines.size() - 1) {
                result = result.append(Component.newline());
            }
        }
        return result;
    }
}
