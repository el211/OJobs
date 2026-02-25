package fr.oreo.oJobs.commands;

import fr.oreo.oJobs.OJobs;
import fr.oreo.oJobs.models.Booster;
import fr.oreo.oJobs.models.Job;
import fr.oreo.oJobs.utils.MessageUtil;
import fr.oreo.oJobs.utils.XpUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;


public class JobsCommand implements CommandExecutor {

    private final OJobs plugin;

    public JobsCommand(OJobs plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (requirePlayer(sender)) {
                plugin.getGuiManager().openMainMenu((Player) sender);
            }
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "join" -> {
                if (!requirePlayer(sender)) return true;
                if (args.length < 2) { usage(sender, "/jobs join <job>"); return true; }
                Player p = (Player) sender;
                plugin.getJobManager().getJob(args[1]).ifPresentOrElse(
                        job -> plugin.getPlayerDataManager().joinJob(p, job),
                        () -> MessageUtil.send(p, "job-not-found", Map.of("job", args[1])));
            }

            case "leave" -> {
                if (!requirePlayer(sender)) return true;
                if (args.length < 2) { usage(sender, "/jobs leave <job>"); return true; }
                Player p = (Player) sender;
                plugin.getJobManager().getJob(args[1]).ifPresentOrElse(
                        job -> plugin.getPlayerDataManager().leaveJob(p, job),
                        () -> MessageUtil.send(p, "job-not-found", Map.of("job", args[1])));
            }

            case "stats" -> {
                if (!requirePlayer(sender)) return true;
                Player p = (Player) sender;
                sendStats(p, p.getName());
            }

            case "top" -> {
                if (!requirePlayer(sender)) return true;
                plugin.getGuiManager().openLeaderboard((Player) sender);
            }

            case "admin" -> {
                if (!requirePlayer(sender)) return true;
                if (!sender.hasPermission("ojobs.admin")) { noPermission(sender); return true; }
                plugin.getGuiManager().openAdminGui((Player) sender);
            }

            case "reload" -> {
                if (!sender.hasPermission("ojobs.reload")) { noPermission(sender); return true; }
                plugin.reload();
                MessageUtil.sendRaw(sender instanceof Player p ? p : null,
                        "<green>oJobs reloaded successfully.", Map.of());
                if (!(sender instanceof Player)) sender.sendMessage("oJobs reloaded.");
            }

            case "giveexp", "givexp" -> {
                if (!sender.hasPermission("ojobs.giveexp")) { noPermission(sender); return true; }
                if (args.length < 4) { usage(sender, "/jobs giveexp <player> <job> <amount>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { notFound(sender, args[1]); return true; }
                double amount;
                try { amount = Double.parseDouble(args[3]); }
                catch (NumberFormatException e) { usage(sender, "/jobs giveexp <player> <job> <amount>"); return true; }
                plugin.getPlayerDataManager().giveXpDirect(target.getUniqueId(), args[2].toLowerCase(), amount);
                sender.sendMessage("§aGave " + amount + " XP to " + target.getName() + " in job " + args[2]);
            }

            case "setlevel" -> {
                if (!sender.hasPermission("ojobs.setlevel")) { noPermission(sender); return true; }
                if (args.length < 4) { usage(sender, "/jobs setlevel <player> <job> <level>"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { notFound(sender, args[1]); return true; }
                int level;
                try { level = Integer.parseInt(args[3]); }
                catch (NumberFormatException e) { usage(sender, "/jobs setlevel <player> <job> <level>"); return true; }
                plugin.getPlayerDataManager().setLevel(target.getUniqueId(), args[2].toLowerCase(), level);
                sender.sendMessage("§aSet " + target.getName() + "'s " + args[2] + " level to " + level);
            }

            case "reset" -> {
                if (!sender.hasPermission("ojobs.reset")) { noPermission(sender); return true; }
                if (args.length < 2) { usage(sender, "/jobs reset <player> [job]"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { notFound(sender, args[1]); return true; }
                if (args.length >= 3) {
                    plugin.getPlayerDataManager().resetJob(target.getUniqueId(), args[2].toLowerCase());
                    sender.sendMessage("§aReset " + target.getName() + "'s " + args[2] + " job.");
                } else {
                    plugin.getPlayerDataManager().resetAllJobs(target.getUniqueId());
                    sender.sendMessage("§aReset all jobs for " + target.getName() + ".");
                }
            }

            case "prestige" -> {
                if (!requirePlayer(sender)) return true;
                Player p = (Player) sender;
                if (!p.hasPermission("ojobs.prestige")) { noPermission(sender); return true; }
                if (args.length < 2) { usage(sender, "/jobs prestige <job>"); return true; }
                plugin.getJobManager().getJob(args[1]).ifPresentOrElse(job -> {
                    plugin.getPlayerDataManager().getPlayerData(p.getUniqueId()).ifPresentOrElse(data -> {
                        if (!plugin.getPrestigeManager().canPrestige(data, job)) {
                            MessageUtil.send(p, "prestige-not-ready",
                                    Map.of("job", job.getDisplayName(), "level", String.valueOf(job.getMaxLevel())));
                        } else {
                            plugin.getPrestigeManager().prestige(p, job);
                        }
                    }, () -> MessageUtil.send(p, "data-not-loaded"));
                }, () -> MessageUtil.send(p, "job-not-found", Map.of("job", args[1])));
            }

            case "booster" -> {
                if (!sender.hasPermission("ojobs.booster")) { noPermission(sender); return true; }
                handleBooster(sender, args);
            }

            case "help", "?" -> sendHelp(sender);

            default -> sendHelp(sender);
        }
        return true;
    }


    private void handleBooster(CommandSender sender, String[] args) {
        if (args.length < 2) { sendBoosterHelp(sender); return; }
        switch (args[1].toLowerCase()) {
            case "add" -> {
                if (args.length < 5) { usage(sender, "/jobs booster add <global|job|player> <target> <multiplier> <seconds|-1>"); return; }
                String scope = args[2].toLowerCase();
                double mult;
                long sec;
                try {
                    mult = Double.parseDouble(args[args.length - 2]);
                    sec  = Long.parseLong(args[args.length - 1]);
                } catch (NumberFormatException e) {
                    usage(sender, "/jobs booster add global <multiplier> <seconds>"); return;
                }
                long durationMs = sec == -1 ? -1 : sec * 1000L;
                switch (scope) {
                    case "global" -> {
                        var b = plugin.getBoosterManager().addGlobalBooster(mult, durationMs);
                        sender.sendMessage(b != null ? "§aGlobal booster added! ID: " + b.getId() : "§cFailed to add booster (limit reached?).");
                    }
                    case "job" -> {
                        if (args.length < 6) { usage(sender, "/jobs booster add job <jobid> <multiplier> <seconds>"); return; }
                        var b = plugin.getBoosterManager().addJobBooster(args[3], mult, durationMs);
                        sender.sendMessage(b != null ? "§aJob booster added for " + args[3] + "! ID: " + b.getId() : "§cFailed.");
                    }
                    case "player" -> {
                        Player target = Bukkit.getPlayer(args[3]);
                        if (target == null) { notFound(sender, args[3]); return; }
                        var b = plugin.getBoosterManager().addPlayerBooster(target.getUniqueId(), mult, durationMs);
                        sender.sendMessage(b != null ? "§aPlayer booster for " + target.getName() + ". ID: " + b.getId() : "§cFailed.");
                    }
                }
            }
            case "list" -> {
                var list = plugin.getBoosterManager().getActiveBoosters();
                if (list.isEmpty()) { sender.sendMessage("§eNo active boosters."); return; }
                sender.sendMessage("§e--- Active Boosters ---");
                for (Booster b : list) {
                    sender.sendMessage("§7ID: §f" + b.getId() + " §7Type: §f" + b.getType()
                            + " §7x§f" + b.getMultiplier() + " §7Remaining: §f" + b.getRemainingFormatted());
                }
            }
            case "remove" -> {
                if (args.length < 3) { usage(sender, "/jobs booster remove <id>"); return; }
                boolean removed = plugin.getBoosterManager().removeBooster(args[2]);
                sender.sendMessage(removed ? "§aBooster removed." : "§cBooster not found.");
            }
            default -> sendBoosterHelp(sender);
        }
    }


    private void sendStats(Player player, String targetName) {
        plugin.getPlayerDataManager().getPlayerData(player.getUniqueId()).ifPresent(data -> {
            player.sendMessage("§6=== " + targetName + "'s Jobs ===");
            if (data.getJobs().isEmpty()) {
                player.sendMessage("§7No jobs joined.");
                return;
            }
            data.getJobs().forEach((jobId, jd) ->
                plugin.getJobManager().getJob(jobId).ifPresent(job -> {
                    double xpReq = XpUtil.getXpRequired(job, jd.getLevel());
                    player.sendMessage("§e" + job.getDisplayName() +
                            " §7| Lv §f" + jd.getLevel() +
                            " §7| XP: §f" + XpUtil.formatXp(jd.getXp()) + "§7/§f" + XpUtil.formatXp(xpReq) +
                            (jd.getPrestige() > 0 ? " §7| P§6" + jd.getPrestige() : ""));
                })
            );
        });
    }


    private boolean requirePlayer(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cThis command can only be used by players.");
            return false;
        }
        return true;
    }

    private void noPermission(CommandSender sender) {
        sender.sendMessage("§cYou don't have permission to do that.");
    }

    private void notFound(CommandSender sender, String name) {
        sender.sendMessage("§cPlayer '" + name + "' is not online.");
    }

    private void usage(CommandSender sender, String usage) {
        sender.sendMessage("§cUsage: " + usage);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("§6=== oJobs Commands ===");
        sender.sendMessage("§e/jobs §7- Open jobs menu");
        sender.sendMessage("§e/jobs join <job> §7- Join a job");
        sender.sendMessage("§e/jobs leave <job> §7- Leave a job");
        sender.sendMessage("§e/jobs stats §7- View your stats");
        sender.sendMessage("§e/jobs top §7- View leaderboard");
        sender.sendMessage("§e/jobs prestige <job> §7- Prestige a job");
        if (sender.hasPermission("ojobs.admin")) {
            sender.sendMessage("§c/jobs admin §7- Open admin panel");
            sender.sendMessage("§c/jobs reload §7- Reload plugin");
            sender.sendMessage("§c/jobs giveexp <player> <job> <amount>");
            sender.sendMessage("§c/jobs setlevel <player> <job> <level>");
            sender.sendMessage("§c/jobs reset <player> [job]");
            sender.sendMessage("§c/jobs booster add/list/remove");
        }
    }

    private void sendBoosterHelp(CommandSender sender) {
        sender.sendMessage("§6=== Booster Commands ===");
        sender.sendMessage("§e/jobs booster add global <multiplier> <seconds>");
        sender.sendMessage("§e/jobs booster add job <jobid> <multiplier> <seconds>");
        sender.sendMessage("§e/jobs booster add player <player> <multiplier> <seconds>");
        sender.sendMessage("§e/jobs booster list");
        sender.sendMessage("§e/jobs booster remove <id>");
    }
}
