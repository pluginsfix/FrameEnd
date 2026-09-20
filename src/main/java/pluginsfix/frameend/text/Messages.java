package pluginsfix.frameend.text;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Messages {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private final JavaPlugin plugin;
    private final Map<String, List<String>> messages = new HashMap<>();

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        messages.clear();

        for (String key : config.getKeys(true)) {
            if (config.isList(key)) {
                messages.put(key, config.getStringList(key));
            } else if (config.isString(key)) {
                messages.put(key, Collections.singletonList(config.getString(key)));
            }
        }
    }

    public void send(CommandSender sender, String key, Placeholder... placeholders) {
        List<String> lines = messages.get(key);
        if (lines == null || lines.isEmpty()) {
            return;
        }

        for (String line : lines) {
            String formatted = applyPlaceholders(line, placeholders);
            if (formatted.startsWith("[message] ")) {
                String text = formatted.substring(10);
                sender.sendMessage(colorize(text));
            } else if (formatted.startsWith("[sound] ") && sender instanceof Player player) {
                String soundName = formatted.substring(8).trim();
                playSound(player, soundName);
            }
        }
    }

    public void broadcast(String key, Placeholder... placeholders) {
        List<String> lines = messages.get(key);
        if (lines == null || lines.isEmpty()) {
            return;
        }

        for (String line : lines) {
            String formatted = applyPlaceholders(line, placeholders);
            if (formatted.startsWith("[message] ")) {
                Component comp = colorize(formatted.substring(10));
                Bukkit.broadcast(comp);
            } else if (formatted.startsWith("[sound] ")) {
                String soundName = formatted.substring(8).trim();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playSound(player, soundName);
                }
            }
        }
    }

    public static Component colorize(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(sb, "§x§" + hexCode.charAt(0) + "§" + hexCode.charAt(1)
                    + "§" + hexCode.charAt(2) + "§" + hexCode.charAt(3)
                    + "§" + hexCode.charAt(4) + "§" + hexCode.charAt(5));
        }
        matcher.appendTail(sb);

        String legacy = sb.toString().replace('&', '§');
        return LegacyComponentSerializer.legacySection().deserialize(legacy);
    }

    public static String colorizeString(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        Matcher matcher = HEX_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String hexCode = matcher.group(1);
            matcher.appendReplacement(sb, "§x§" + hexCode.charAt(0) + "§" + hexCode.charAt(1)
                    + "§" + hexCode.charAt(2) + "§" + hexCode.charAt(3)
                    + "§" + hexCode.charAt(4) + "§" + hexCode.charAt(5));
        }
        matcher.appendTail(sb);

        return sb.toString().replace('&', '§');
    }

    private String applyPlaceholders(String line, Placeholder... placeholders) {
        String result = line;
        for (Placeholder placeholder : placeholders) {
            result = result.replace("{" + placeholder.key() + "}", placeholder.value());
        }
        return result;
    }

    private void playSound(Player player, String soundName) {
        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public record Placeholder(String key, String value) {
        public static Placeholder of(String key, Object value) {
            return new Placeholder(key, String.valueOf(value));
        }
    }
}
