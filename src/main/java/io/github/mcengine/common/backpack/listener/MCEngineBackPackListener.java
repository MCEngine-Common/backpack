package io.github.mcengine.common.backpack.listener;

import io.github.mcengine.api.backpack.MCEngineBackPackApi;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Listener that connects player interactions and inventory lifecycle events to the
 * {@link MCEngineBackPackApi} so backpacks can be opened and saved seamlessly.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Open a virtual backpack inventory on right-clicking a backpack item (block target or air, main hand or off-hand).</li>
 *   <li>Persist backpack contents on inventory close.</li>
 *   <li>Prevent putting backpacks inside backpacks (to avoid recursion/dupe issues).</li>
 *   <li>Clean up per-player state on disconnect.</li>
 * </ul>
 */
public class MCEngineBackPackListener implements Listener {

    /** Reference to the owning plugin, used for logging and scheduler-safe operations. */
    private final Plugin plugin;

    /** API instance that performs create/open/save operations for backpack items. */
    private final MCEngineBackPackApi api;

    /**
     * Tracks the currently opened backpack item for each player while its GUI is open.
     * Key: player's UUID; Value: the {@link ItemStack} that was used to open the backpack.
     */
    private final Map<UUID, ItemStack> openBackpacks = new HashMap<>();

    /**
     * Creates a new listener bound to a plugin and its backpack API.
     *
     * @param plugin the plugin instance
     */
    public MCEngineBackPackListener(Plugin plugin) {
        this.plugin = plugin;
        this.api = new MCEngineBackPackApi(plugin);
    }

    /**
     * Handles right-clicks to open a backpack GUI when a backpack item is used.
     *
     * <p>Changes:</p>
     * <ul>
     *   <li>Accepts both {@link Action#RIGHT_CLICK_AIR} and {@link Action#RIGHT_CLICK_BLOCK}.</li>
     *   <li>Accepts both hands: {@link EquipmentSlot#HAND} and {@link EquipmentSlot#OFF_HAND}.</li>
     *   <li>Uses the item actually in the triggering hand; if both hands hold backpacks,
     *   the main-hand event is preferred to avoid double-open.</li>
     *   <li>Runs at {@link EventPriority#HIGHEST} and does <b>not</b> ignore cancelled events,
     *   ensuring other plugins cancelling interact-in-air won’t prevent opening.</li>
     * </ul>
     *
     * @param event the interaction event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onRightClickOpenBackpack(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();

        // Prefer main hand if both hands contain backpacks to avoid double-firing
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off  = player.getInventory().getItemInOffHand();

        // Determine which hand triggered and pick the corresponding item
        EquipmentSlot hand = event.getHand();
        if (hand == null) return; // Safety guard

        // If off-hand triggered but main-hand also holds a backpack, let the main-hand event handle it
        if (hand == EquipmentSlot.OFF_HAND && api.isBackpack(main)) {
            return;
        }

        ItemStack usedItem = (hand == EquipmentSlot.HAND) ? main : off;
        if (usedItem == null || !api.isBackpack(usedItem)) return;

        event.setCancelled(true); // Prevent placing/using the head as a normal item

        Inventory inv = api.openBackpack(usedItem);
        // Track which item should receive the saved contents on close
        openBackpacks.put(player.getUniqueId(), usedItem);
        player.openInventory(inv);
    }

    /**
     * Saves the contents of an open backpack back into the backpack item when the player closes it.
     *
     * @param event the inventory close event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBackpackClose(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        UUID uuid = player.getUniqueId();

        ItemStack backpackItem = openBackpacks.get(uuid);
        if (backpackItem == null) return; // Not a tracked backpack close

        // Save the inventory contents back into the item meta
        Inventory closed = event.getInventory();
        api.saveBackpack(backpackItem, closed);

        // Clean up mapping after save
        openBackpacks.remove(uuid);
    }

    /**
     * Prevents placing backpacks inside backpack inventories to avoid recursion/duplication.
     *
     * @param event the inventory click event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // If the player isn't currently interacting with a tracked backpack, ignore
        if (!openBackpacks.containsKey(player.getUniqueId())) return;

        // Determine if the click targets the top inventory (the open backpack GUI)
        Inventory clickedInv = event.getClickedInventory();
        Inventory topInv = event.getView().getTopInventory();
        if (clickedInv == null || topInv == null) return;

        // Only apply restriction within the backpack GUI (top inventory)
        if (!clickedInv.equals(topInv)) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        // If trying to place or swap a backpack item into the backpack GUI, cancel it
        if ((cursor != null && api.isBackpack(cursor)) || (current != null && api.isBackpack(current))) {
            event.setCancelled(true);
        }
    }

    /**
     * Ensures per-player tracking is cleared if a player disconnects while a backpack is open.
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        openBackpacks.remove(event.getPlayer().getUniqueId());
    }
}
