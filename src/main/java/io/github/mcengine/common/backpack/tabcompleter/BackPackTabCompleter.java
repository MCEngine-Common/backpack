package io.github.mcengine.common.backpack.tabcompleter;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tab completer for the {@code /backpack} command using two-level syntax.
 *
 * <p>Correct completion path:</p>
 * <pre>{@code
 * /backpack default give [player-online] [hdb-id] [1-6]
 * }</pre>
 *
 * <p>This completer supports both raw and dispatcher-sliced modes:
 * <ul>
 *   <li><b>Raw:</b> args include {@code default} (e.g., {@code ["default", "give", ...]}).</li>
 *   <li><b>Sub-scope:</b> dispatcher consumed {@code default} already (e.g., {@code ["give", ...]}).</li>
 * </ul>
 * It normalizes by detecting and skipping the {@code default} token when present.</p>
 */
public class BackPackTabCompleter implements TabCompleter {

    /**
     * Provides tab completion for {@code /backpack default give ...}.
     *
     * @param sender  command sender
     * @param command command object
     * @param alias   alias used
     * @param args    command arguments
     * @return list of suggestions
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // Handle the root "/backpack " case (before "default") — suggest "default"
        if (args.length == 1 && !"default".equalsIgnoreCase(args[0])) {
            String prefix = args[0].toLowerCase();
            List<String> out = new ArrayList<>();
            if ("default".startsWith(prefix)) out.add("default");
            return out;
        }

        // Normalize to "sub-scope" by skipping the 'default' token if present.
        int offset = (args.length > 0 && "default".equalsIgnoreCase(args[0])) ? 1 : 0;
        int stage = args.length - offset; // 1: give, 2: player, 3: hdb, 4: rows

        // If we aren't in or after 'default', nothing more to suggest
        if (offset == 0 && args.length == 1) {
            return Collections.emptyList();
        }

        // Guard permission once we get into actionable subcommands
        boolean canGive = sender.hasPermission("mcengine.backpack.give");

        List<String> out = new ArrayList<>();

        switch (stage) {
            case 1 -> {
                // /backpack default <here>  OR  (dispatcher-sliced) /backpack default <here>
                String prefix = args[offset].toLowerCase();
                if (canGive && "give".startsWith(prefix)) out.add("give");
                return out;
            }
            case 2 -> {
                // /backpack default give <player>
                if (!canGive) return Collections.emptyList();
                String prefix = args[offset + 1].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(prefix)) {
                        out.add(p.getName());
                    }
                }
                return out;
            }
            case 3 -> {
                // /backpack default give <player> <hdb-id>  -> no suggestions
                if (!canGive) return Collections.emptyList();
                return Collections.emptyList();
            }
            case 4 -> {
                // /backpack default give <player> <hdb-id> <rows>
                if (!canGive) return Collections.emptyList();
                String prefix = args[offset + 3];
                for (int i = 1; i <= 6; i++) {
                    String s = Integer.toString(i);
                    if (s.startsWith(prefix)) out.add(s);
                }
                return out;
            }
            default -> {
                return Collections.emptyList();
            }
        }
    }
}
