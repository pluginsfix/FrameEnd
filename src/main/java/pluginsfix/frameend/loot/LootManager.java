package pluginsfix.frameend.loot;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class LootManager {
    public static final String CATEGORY_COMMON = "common_anchors";
    public static final String CATEGORY_SECRET = "secret_rift";
    public static final String CATEGORY_DRAGON = "dragon_drops";

    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration yaml;
    private final Map<String, List<ItemStack>> customLootCache = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public LootManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "custom_loot.yml");
        load();
    }

    public void load() {
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException ignored) {
            }
        }
        yaml = YamlConfiguration.loadConfiguration(file);
        customLootCache.clear();

        loadCategory(CATEGORY_COMMON);
        loadCategory(CATEGORY_SECRET);
        loadCategory(CATEGORY_DRAGON);
    }

    private void loadCategory(String category) {
        List<?> rawList = yaml.getList(category);
        List<ItemStack> items = new ArrayList<>();
        if (rawList != null) {
            for (Object obj : rawList) {
                if (obj instanceof ItemStack stack) {
                    items.add(stack.clone());
                }
            }
        }
        customLootCache.put(category, items);
    }

    public List<ItemStack> getCategoryItems(String category) {
        List<ItemStack> list = customLootCache.get(category);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public void saveCategory(String category, List<ItemStack> items) {
        List<ItemStack> cleaned = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                cleaned.add(item.clone());
            }
        }
        customLootCache.put(category, cleaned);
        yaml.set(category, cleaned);
        try {
            yaml.save(file);
        } catch (IOException ignored) {
        }
    }

    public boolean hasCustomLoot(String category) {
        List<ItemStack> list = customLootCache.get(category);
        return list != null && !list.isEmpty();
    }

    public ItemStack getRandomLootItem(String category) {
        List<ItemStack> list = customLootCache.get(category);
        if (list == null || list.isEmpty()) return null;
        int idx = random.nextInt(list.size());
        return list.get(idx).clone();
    }
}
