package io.github.mcengine.common.backpack.command;

import io.github.mcengine.common.backpack.MCEngineBackPackCommon;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Command executor for {@code /backpack} administrative actions.
 *
 * <p>New syntax (two-level):</p>
 * <pre>{@code
 * /backpack default give <player> <hdb-id> <rows(1-6)>
 * }</pre>
 *
 * <p>Permissions:</p>
 * <ul>
 *   <li>{@code mcengine.backpack.give} – allows using the {@code default give} action.</li>
 * </ul>
 */
public class BackPackCommand implements CommandExecutor {

    /** Owning plugin instance for context and messaging. */
    private final Plugin plugin;

    /** Common dispatcher/API facade for backpack operations. */
    private final MCEngineBackPackCommon common;

    /**
     * Creates a new command handler.
     *
     * @param plugin the Bukkit plugin instance
     */
    public BackPackCommand(Plugin plugin) {
        this.plugin = plugin;
        // Use existing singleton if available; otherwise initialize to ensure command usability.
        MCEngineBackPackCommon existing = MCEngineBackPackCommon.getApi();
        this.common = (existing != null) ? existing : new MCEngineBackPackCommon(plugin);
    }

    /**
     * Handles the {@code /backpack} command.
     *
     * @param sender  command sender
     * @param command command object
     * @param label   alias used
     * @param args    command arguments
     * @return true if handled; false to show usage by Bukkit
    */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /backpack
        if (args.length == 0) {
            sendUsage(sender, label);
            return true;
        }

        String level1 = args[0].toLowerCase(); // "default"
        switch (level1) {
            case "default" -> {
                // /backpack default
                if (args.length == 1) {
                    sendUsage(sender, label);
                    return true;
                }

                String level2 = args[1].toLowerCase(); // "give"
                switch (level2) {
                    case "give" -> {
                        if (!sender.hasPermission("mcengine.backpack.give")) {
                            sender.sendMessage(ChatColor.RED + "You do not have permission: mcengine.backpack.give");
                            return true;
                        }
                        // /backpack default give <player> <hdb-id> <rows>
                        if (args.length < 5) {
                            sender.sendMessage(ChatColor.YELLOW + "Usage: " + ChatColor.WHITE + "/" + label + " default give <player> <hdb-id> <rows(1-6)>");
                            return true;
                        }

                        String playerName = args[2];
                        Player target = Bukkit.getPlayerExact(playerName);
                        if (target == null) {
                            sender.sendMessage(ChatColor.RED + "Player not found or not online: " + playerName);
                            return true;
                        }

                        String hdbId = args[3];
                        int rows;
                        try {
                            rows = Integer.parseInt(args[4]);
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "Rows must be a number between 1 and 6.");
                            return true;
                        }
                        if (rows < 1 || rows > 6) {
                            sender.sendMessage(ChatColor.RED + "Rows must be between 1 and 6.");
                            return true;
                        }

                        int size = rows * 9;
                        ItemStack backpack = common.createBackpack("Backpack", hdbId, size);

                        // Try to add to inventory; drop remainder at feet if full.
                        var remainder = target.getInventory().addItem(backpack);
                        if (!remainder.isEmpty()) {
                            Location loc = target.getLocation();
                            remainder.values().forEach(item -> target.getWorld().dropItemNaturally(loc, item));
                        }

                        sender.sendMessage(ChatColor.GREEN + "Gave a " + rows + " row backpack to " + target.getName() + ".");
                        if (!sender.equals(target)) {
                            target.sendMessage(ChatColor.GREEN + "You received a " + rows + " row backpack from " + sender.getName() + ".");
                        }
                        return true;
                    }
                    default -> {
                        sendUsage(sender, label);
                        return true;
                    }
                }
            }
            default -> {
                sendUsage(sender, label);
                return true;
            }
        }
    }

    /**
     * Sends short usage help to the sender.
     *
     * @param sender target to message
     * @param label  command label used
     */
    private void sendUsage(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.YELLOW + "Backpack commands:");
        sender.sendMessage(ChatColor.WHITE + "/" + label + " default give <player> <hdb-id> <rows(1-6)>" + ChatColor.GRAY + " - give a backpack item");
    }
}
