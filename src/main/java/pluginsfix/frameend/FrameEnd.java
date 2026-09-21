package pluginsfix.frameend;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import pluginsfix.frameend.command.DragonCompassCommand;
import pluginsfix.frameend.command.FrameEndCommand;
import pluginsfix.frameend.compass.DragonCompassItemFactory;
import pluginsfix.frameend.compass.DragonCompassManager;
import pluginsfix.frameend.config.FrameEndConfig;
import pluginsfix.frameend.egg.DragonEggItemFactory;
import pluginsfix.frameend.egg.PlacedEggManager;
import pluginsfix.frameend.event.EndWorldEventManager;
import pluginsfix.frameend.hologram.HologramManager;
import pluginsfix.frameend.hook.PlaceholderApiHook;
import pluginsfix.frameend.hook.PlayerPointsHook;
import pluginsfix.frameend.hook.VaultEconomyHook;
import pluginsfix.frameend.listener.AnchorInteractListener;
import pluginsfix.frameend.listener.CompassUseListener;
import pluginsfix.frameend.listener.DragonDamageListener;
import pluginsfix.frameend.listener.EggInteractionListener;
import pluginsfix.frameend.listener.IslandBoundaryListener;
import pluginsfix.frameend.listener.LootEditorListener;
import pluginsfix.frameend.listener.PortalAccessListener;
import pluginsfix.frameend.loot.LootManager;
import pluginsfix.frameend.storage.SqliteStorage;
import pluginsfix.frameend.storage.Storage;
import pluginsfix.frameend.text.Messages;

public final class FrameEnd extends JavaPlugin {
    private FrameEndConfig config;
    private Messages messages;
    private Storage storage;
    private HologramManager hologramManager;
    private LootManager lootManager;
    private VaultEconomyHook vaultHook;
    private PlayerPointsHook pointsHook;
    private DragonEggItemFactory eggItemFactory;
    private DragonCompassItemFactory compassItemFactory;
    private PlacedEggManager placedEggManager;
    private DragonCompassManager compassManager;
    private EndWorldEventManager eventManager;

    @Override
    public void onEnable() {
        this.config = new FrameEndConfig(this);
        this.messages = new Messages(this);
        this.lootManager = new LootManager(this);

        this.storage = new SqliteStorage(getDataFolder(), getLogger());
        this.storage.init();

        this.hologramManager = new HologramManager();

        this.vaultHook = new VaultEconomyHook(this);
        this.pointsHook = new PlayerPointsHook();

        this.eggItemFactory = new DragonEggItemFactory(this);
        this.compassItemFactory = new DragonCompassItemFactory(this, config);

        this.placedEggManager = new PlacedEggManager(this, config, storage, messages, vaultHook, pointsHook, eggItemFactory, hologramManager);
        this.placedEggManager.start();

        this.compassManager = new DragonCompassManager(config, storage, messages, placedEggManager);

        this.eventManager = new EndWorldEventManager(this, config, messages, eggItemFactory, hologramManager, lootManager, pointsHook);
        this.eventManager.init();

        registerCommands();
        registerListeners();
        registerHooks();
    }

    private void registerCommands() {
        PluginCommand frameEndCmd = getCommand("frameend");
        if (frameEndCmd != null) {
            FrameEndCommand executor = new FrameEndCommand(config, messages, eventManager, eggItemFactory, compassItemFactory, lootManager);
            frameEndCmd.setExecutor(executor);
            frameEndCmd.setTabCompleter(executor);
        }

        PluginCommand compassCmd = getCommand("dragoncompass");
        if (compassCmd != null) {
            DragonCompassCommand executor = new DragonCompassCommand(compassManager, messages);
            compassCmd.setExecutor(executor);
            compassCmd.setTabCompleter(executor);
        }
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        pm.registerEvents(new PortalAccessListener(config, eventManager, messages), this);
        pm.registerEvents(new IslandBoundaryListener(config, eventManager, messages), this);
        pm.registerEvents(new AnchorInteractListener(eventManager), this);
        pm.registerEvents(new DragonDamageListener(eventManager), this);
        pm.registerEvents(new EggInteractionListener(config, eventManager, placedEggManager, eggItemFactory, storage, messages, vaultHook, pointsHook), this);
        pm.registerEvents(new CompassUseListener(compassItemFactory, compassManager), this);
        pm.registerEvents(new LootEditorListener(), this);
    }

    private void registerHooks() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PlaceholderApiHook(eventManager, placedEggManager, storage).register();
        }
    }

    @Override
    public void onDisable() {
        if (eventManager != null) {
            eventManager.shutdown();
        }
        if (placedEggManager != null) {
            placedEggManager.stop();
        }
        if (hologramManager != null) {
            hologramManager.clearAll();
        }
        if (storage != null) {
            storage.close();
        }
    }
}
