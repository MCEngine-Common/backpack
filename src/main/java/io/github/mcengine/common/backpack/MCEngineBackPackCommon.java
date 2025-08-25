package io.github.mcengine.common.backpack;

import io.github.mcengine.api.backpack.MCEngineBackPackApi;
import io.github.mcengine.api.core.util.MCEngineCoreApiDispatcher;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Common logic and command-dispatch facade for the MCEngine Backpack plugin.
 *
 * <p>This class centralizes access to {@link MCEngineBackPackApi} and exposes a
 * {@link MCEngineCoreApiDispatcher}-backed command registration surface so
 * Spigot/Paper entry points can bind Bukkit commands to namespaced handlers
 * without duplicating wiring code.</p>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *   <li>Provide a singleton access point for module-wide services.</li>
 *   <li>Expose a command dispatcher API (namespace, subcommands, tab completion).</li>
 *   <li>Offer convenience methods that delegate to {@link MCEngineBackPackApi}:</li>
 *   <ul>
 *     <li>Create backpack items.</li>
 *     <li>Open and save backpack inventories.</li>
 *     <li>Identify backpack items.</li>
 *   </ul>
 * </ul>
 */
public class MCEngineBackPackCommon {

    /** Singleton instance of this common backpack manager. */
    private static MCEngineBackPackCommon instance;

    /** Associated Bukkit plugin instance used for configuration, logging, and scheduling. */
    private final Plugin plugin;

    /** Core Backpack API used to create, open, and persist backpack item data. */
    private final MCEngineBackPackApi backpackApi;

    /** Internal command dispatcher used to register namespaces and subcommands. */
    private final MCEngineCoreApiDispatcher dispatcher;

    /**
     * Constructs a new common backpack manager and initializes its dispatcher and API.
     * <p>
     * This constructor also validates that the HeadDatabase plugin is present,
     * since backpacks rely on custom heads for their appearance. If HeadDatabase
     * is not detected, the owning plugin is disabled immediately.
     * </p>
     *
     * @param plugin The owning Bukkit {@link Plugin} instance.
     */
    public MCEngineBackPackCommon(Plugin plugin) {
        instance = this;
        this.plugin = plugin;

        // --- Dependency check: HeadDatabase ---
        if (Bukkit.getPluginManager().getPlugin("HeadDatabase") == null) {
            plugin.getLogger().severe("HeadDatabase plugin not found! MCEngineBackPack requires HeadDatabase to function.");
            Bukkit.getPluginManager().disablePlugin(plugin);
            throw new IllegalStateException("HeadDatabase dependency missing. Plugin disabled.");
        }
        // --------------------------------------

        this.backpackApi = new MCEngineBackPackApi(plugin);
        this.dispatcher = new MCEngineCoreApiDispatcher();
    }

    /**
     * Returns the global singleton instance.
     *
     * @return The {@link MCEngineBackPackCommon} singleton.
     */
    public static MCEngineBackPackCommon getApi() {
        return instance;
    }

    /**
     * Gets the associated plugin instance.
     *
     * @return The Bukkit plugin.
     */
    public Plugin getPlugin() {
        return plugin;
    }

    /**
     * Direct access to the underlying {@link MCEngineBackPackApi}.
     *
     * @return The backpack API instance.
     */
    public MCEngineBackPackApi getBackpackApi() {
        return backpackApi;
    }

    /* ===========================
     * Dispatcher (command wiring)
     * =========================== */

    /**
     * Registers a command namespace (e.g., {@code "backpack"}) for this plugin's dispatcher.
     *
     * @param namespace Unique namespace for commands.
     */
    public void registerNamespace(String namespace) {
        dispatcher.registerNamespace(namespace);
    }

    /**
     * Binds a Bukkit command (like {@code /backpack}) to the internal dispatcher.
     *
     * @param namespace       The command namespace.
     * @param commandExecutor Fallback executor for unmapped subcommands.
     */
    public void bindNamespaceToCommand(String namespace, CommandExecutor commandExecutor) {
        dispatcher.bindNamespaceToCommand(namespace, commandExecutor);
    }

    /**
     * Registers a subcommand under the specified namespace.
     *
     * @param namespace The command namespace.
     * @param name      Subcommand label.
     * @param executor  Subcommand logic.
     */
    public void registerSubCommand(String namespace, String name, CommandExecutor executor) {
        dispatcher.registerSubCommand(namespace, name, executor);
    }

    /**
     * Registers a tab completer for a subcommand under the specified namespace.
     *
     * @param namespace    The command namespace.
     * @param subcommand   Subcommand label.
     * @param tabCompleter Tab completion logic.
     */
    public void registerSubTabCompleter(String namespace, String subcommand, TabCompleter tabCompleter) {
        dispatcher.registerSubTabCompleter(namespace, subcommand, tabCompleter);
    }

    /**
     * Gets the dispatcher instance to assign as command executor and tab completer.
     *
     * @param namespace Command namespace.
     * @return Command executor for Bukkit command registration.
     */
    public CommandExecutor getDispatcher(String namespace) {
        return dispatcher.getDispatcher(namespace);
    }

    /* ===========================
     * Backpack API conveniences
     * =========================== */

    /**
     * Creates a new backpack {@link ItemStack}.
     *
     * @param backpackName Display name for the backpack.
     * @param textureID    HeadDatabase texture ID for appearance.
     * @param size         Inventory size (must be a valid Bukkit multiple of 9).
     * @return The created backpack item.
     */
    public ItemStack createBackpack(String backpackName, String textureID, int size) {
        return backpackApi.getBackpack(backpackName, textureID, size);
    }

    /**
     * Opens a virtual inventory from the given backpack item.
     *
     * @param backpackItem Backpack {@link ItemStack}.
     * @return Deserialized {@link Inventory} with contents applied.
     */
    public Inventory openBackpack(ItemStack backpackItem) {
        return backpackApi.openBackpack(backpackItem);
    }

    /**
     * Saves an inventory's contents back into the backpack item metadata.
     *
     * @param backpackItem Backpack {@link ItemStack}.
     * @param inventory    Inventory to serialize and store.
     */
    public void saveBackpack(ItemStack backpackItem, Inventory inventory) {
        backpackApi.saveBackpack(backpackItem, inventory);
    }

    /**
     * Determines whether an {@link ItemStack} is a recognized backpack.
     *
     * @param item The item to test.
     * @return {@code true} if the item is a backpack; otherwise {@code false}.
     */
    public boolean isBackpack(ItemStack item) {
        return backpackApi.isBackpack(item);
    }
}
