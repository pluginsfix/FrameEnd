package pluginsfix.frameend.hologram;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import pluginsfix.frameend.domain.AnchorRarity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class HologramManager implements HologramService {
    private final HologramService delegate;

    public HologramManager() {
        Plugin fancyHologramsPlugin = Bukkit.getPluginManager().getPlugin("FancyHolograms");
        if (fancyHologramsPlugin != null && fancyHologramsPlugin.isEnabled()) {
            HologramService fancyService = createFancyHologramsService();
            this.delegate = (fancyService != null) ? fancyService : new DisplayEntityHologramService();
        } else {
            this.delegate = new DisplayEntityHologramService();
        }
    }

    private HologramService createFancyHologramsService() {
        try {
            return new FancyHologramsService();
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void spawnOrUpdate(String id, Location location, List<String> lines) {
        delegate.spawnOrUpdate(id, location, lines);
    }

    @Override
    public void remove(String id) {
        delegate.remove(id);
    }

    public void removeAt(Location location, double radius) {
        if (delegate instanceof DisplayEntityHologramService displayService) {
            displayService.removeAt(location, radius);
        }
    }

    @Override
    public void clearAll() {
        delegate.clearAll();
    }

    public void updateAnchorHologram(String id, Location loc, AnchorRarity rarity, String rarityDisplayName, int hitsLeft, int totalHits) {
        Location holoLoc = loc.clone().add(0.5, 1.4, 0.5);
        List<String> lines = new ArrayList<>();
        if (rarity == AnchorRarity.SECRET_RIFT) {
            lines.add("&#FB8808▶ &#FB8808Секретный Обелиск");
            lines.add("&#FFFF00◆ &fРедкость: " + rarityDisplayName);
            lines.add("&#FFFF00◆ &fОсталось сломать: &#FB8808" + hitsLeft + "&8/&#FFFF00" + totalHits);
        } else {
            lines.add("&#FFFF00▶ &#FFFF00Якорь Возрождения");
            lines.add("&#FFFF00◆ &fРедкость: " + rarityDisplayName);
            lines.add("&#FFFF00◆ &fОсталось сломать: &#FB8808" + hitsLeft + "&8/&#FFFF00" + totalHits);
        }
        spawnOrUpdate(id, holoLoc, lines);
    }

    public void updateCaptureEggHologram(Location loc, int currentHits, int maxHits) {
        Location holoLoc = loc.clone().add(0.5, 1.3, 0.5);
        int remaining = Math.max(0, maxHits - currentHits);
        List<String> lines = List.of(
                "&#FB8808▶ &#FFFF00Яйцо Древнего Дракона",
                "&#FFFF00◆ &fОсталось сломать: &#FB8808" + remaining + " &fударов",
                "&#FFFF00◆ &fПрогресс: &#FFFF00" + currentHits + "&8/&#FFFF00" + maxHits
        );
        spawnOrUpdate("capture_egg", holoLoc, lines);
    }

    public void updatePlacedEggHologram(int id, Location loc, String ownerName, int durability, int maxDurability, double income, String currency) {
        Location holoLoc = loc.clone().add(0.5, 1.3, 0.5);
        List<String> lines = List.of(
                "&#FFFF00▶ &#FFFF00Яйцо Древнего Дракона",
                "&#FFFF00◆ &fВладелец: &#FFFF00" + ownerName,
                "&#FFFF00◆ &fПрочность: &#FB8808" + durability + "&8/&#FFFF00" + maxDurability,
                "&#FFFF00◆ &fДоход: &#FFFF00+" + String.format("%.1f", income) + " " + currency
        );
        spawnOrUpdate("placed_egg_" + id, holoLoc, lines);
    }

    private static final class FancyHologramsService implements HologramService {
        private final Map<String, Object> activeHolograms = new ConcurrentHashMap<>();
        private final Object manager;
        private final java.lang.reflect.Constructor<?> createTextHologramConstructor;
        private final Method addHologramMethod;
        private final Method removeHologramMethod;
        private final Method setTextMethod;
        private final Method getTextDataMethod;

        public FancyHologramsService() throws Exception {
            Class<?> pluginClass = Class.forName("de.oliver.fancyholograms.api.FancyHologramsPlugin");
            Method getPlugin = pluginClass.getMethod("get");
            Object pluginInstance = getPlugin.invoke(null);

            Method getManager = pluginClass.getMethod("getHologramManager");
            this.manager = getManager.invoke(pluginInstance);

            Class<?> managerClass = manager.getClass();
            this.addHologramMethod = managerClass.getMethod("addHologram", Class.forName("de.oliver.fancyholograms.api.hologram.Hologram"));
            this.removeHologramMethod = managerClass.getMethod("removeHologram", Class.forName("de.oliver.fancyholograms.api.hologram.Hologram"));

            Class<?> textDataClass = Class.forName("de.oliver.fancyholograms.api.data.TextHologramData");
            this.createTextHologramConstructor = textDataClass.getConstructor(String.class, Location.class);
            this.setTextMethod = textDataClass.getMethod("setText", List.class);

            Class<?> hologramClass = Class.forName("de.oliver.fancyholograms.api.hologram.Hologram");
            this.getTextDataMethod = hologramClass.getMethod("getData");
        }

        @Override
        public void spawnOrUpdate(String id, Location location, List<String> lines) {
            try {
                Object existing = activeHolograms.get(id);
                if (existing != null) {
                    Object data = getTextDataMethod.invoke(existing);
                    setTextMethod.invoke(data, lines);
                    return;
                }

                Object data = createTextHologramConstructor.newInstance("frameend_" + id, location.clone().add(0, 0.2, 0));
                setTextMethod.invoke(data, lines);

                Class<?> pluginClass = Class.forName("de.oliver.fancyholograms.api.FancyHologramsPlugin");
                Object pluginInstance = pluginClass.getMethod("get").invoke(null);
                Object adapter = pluginClass.getMethod("getHologramAdapter").invoke(pluginInstance);

                Method applyMethod = adapter.getClass().getMethod("apply", Class.forName("de.oliver.fancyholograms.api.data.HologramData"));
                Object hologram = applyMethod.invoke(adapter, data);

                addHologramMethod.invoke(manager, hologram);
                activeHolograms.put(id, hologram);
            } catch (Throwable ignored) {
            }
        }

        @Override
        public void remove(String id) {
            Object holo = activeHolograms.remove(id);
            if (holo != null) {
                try {
                    removeHologramMethod.invoke(manager, holo);
                } catch (Throwable ignored) {
                }
            }
        }

        @Override
        public void clearAll() {
            for (Object holo : activeHolograms.values()) {
                try {
                    removeHologramMethod.invoke(manager, holo);
                } catch (Throwable ignored) {
                }
            }
            activeHolograms.clear();
        }
    }
}
