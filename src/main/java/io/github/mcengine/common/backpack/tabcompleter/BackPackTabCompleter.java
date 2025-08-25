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
 * Tab completer for the {@code /backpack} command.
 *
 * <p>Completions provided for two-level syntax:</p>
 * <ul>
 *   <li>Level 1: {@code default}</li>
 *   <li>Level 2 (under {@code default}): {@code give}</li>
 *   <li>Level 3 (player): online player names</li>
 *   <li>Level 5 (rows): {@code 1-6}</li>
 * </ul>
 *
 * <p>Notes:</p>
 * <ul>
 *   <li>No reliable autocomplete for HeadDatabase IDs (arg 4), so it is omitted.</li>
 * </ul>
 */
public class BackPackTabCompleter implements TabCompleter {

    /**
     * Provides tab completion for {@code /backpack}.
     *
     * @param sender  command sender
     * @param command command object
     * @param alias   alias used
     * @param args    command arguments
     * @return list of suggestions
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();

        // /backpack <arg1>
        if (args.length == 1) {
            if ("default".startsWith(args[0].toLowerCase())) {
                out.add("default");
            }
            return out;
        }

        // /backpack default <arg2>
        if (args.length == 2) {
            if ("default".equalsIgnoreCase(args[0])) {
                if ("give".startsWith(args[1].toLowerCase()) && sender.hasPermission("mcengine.backpack.give")) {
                    out.add("give");
                }
                return out;
            }
            return Collections.emptyList();
        }

        // /backpack default give <player>
        if (args.length == 3) {
            if ("default".equalsIgnoreCase(args[0]) && "give".equalsIgnoreCase(args[1]) && sender.hasPermission("mcengine.backpack.give")) {
                String prefix = args[2].toLowerCase();
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase().startsWith(prefix)) {
                        out.add(p.getName());
                    }
                }
                return out;
            }
            return Collections.emptyList();
        }

        // /backpack default give <player> <hdb-id>
        if (args.length == 4) {
            // No completion for HeadDatabase IDs
            return Collections.emptyList();
        }

        // /backpack default give <player> <hdb-id> <rows>
        if (args.length == 5) {
            if ("default".equalsIgnoreCase(args[0]) && "give".equalsIgnoreCase(args[1]) && sender.hasPermission("mcengine.backpack.give")) {
                String prefix = args[4];
                for (int i = 1; i <= 6; i++) {
                    String s = Integer.toString(i);
                    if (s.startsWith(prefix)) out.add(s);
                }
                return out;
            }
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }
}
