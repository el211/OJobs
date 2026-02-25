package fr.oreo.oJobs.commands;

import fr.oreo.oJobs.OJobs;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class JobsTabCompleter implements TabCompleter {

    private final OJobs plugin;

    private static final List<String> SUBCOMMANDS = List.of(
            "join", "leave", "stats", "top", "admin", "reload",
            "giveexp", "setlevel", "reset", "prestige", "booster", "help"
    );

    public JobsTabCompleter(OJobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command,
                                      String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "join", "leave", "prestige" ->
                        completions.addAll(plugin.getJobManager().getJobMap().keySet());
                case "giveexp", "setlevel", "reset" ->
                        Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                case "stats" -> {
                    Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                    completions.add(0, sender instanceof Player p ? p.getName() : "");
                }
                case "top" ->
                        completions.addAll(plugin.getJobManager().getJobMap().keySet());
                case "booster" ->
                        completions.addAll(List.of("add", "list", "remove"));
            }
        }

        if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "giveexp", "setlevel", "reset" ->
                        completions.addAll(plugin.getJobManager().getJobMap().keySet());
                case "booster" -> {
                    if (args[1].equalsIgnoreCase("add")) {
                        completions.addAll(List.of("global", "job", "player"));
                    } else if (args[1].equalsIgnoreCase("remove")) {
                        plugin.getBoosterManager().getActiveBoosters()
                                .forEach(b -> completions.add(b.getId()));
                    }
                }
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("booster") && args[1].equalsIgnoreCase("add")) {
            switch (args[2].toLowerCase()) {
                case "job"    -> completions.addAll(plugin.getJobManager().getJobMap().keySet());
                case "player" -> Bukkit.getOnlinePlayers().forEach(p -> completions.add(p.getName()));
                case "global" -> completions.add("<multiplier>");
            }
        }

        return filter(completions, args[args.length - 1]);
    }

    private List<String> filter(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(lower))
                .collect(Collectors.toList());
    }
}
