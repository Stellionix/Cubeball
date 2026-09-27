package me.crylonz;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static me.crylonz.CubeBall.*;

public class CBCommandExecutor implements CommandExecutor {

    public CBCommandExecutor() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("cb") && ClubCommands.handle(sender, args)) return true;
        if (args.length > 1 && (args[0].equalsIgnoreCase("generate") || args[0].equalsIgnoreCase("remove")) && args[1].equals(BALL_MATCH_ID)) {
            sender.sendMessage("[CubeBall] This ball ID is reserved for matches."); return true;
        }
        Player player;
        if ((sender instanceof Player)) {
            player = (Player) sender;


            if (cmd.getName().equalsIgnoreCase("cb")) {
                if (args.length == 1) {
                    if (args[0].equalsIgnoreCase("reload") && hasManagePermission(player)) {
                        reloadPluginConfiguration();
                        player.sendMessage("[Cubeball] " + ChatColor.GREEN + "Configuration reloaded.");
                    } else if (args[0].equalsIgnoreCase("match") && player.hasPermission("cubeball.manage")) {
                        if (match != null) match.stop();
                        match = new Match();
                        match.scanSpawn(player);
                    } else if (args[0].equalsIgnoreCase("start") && player.hasPermission("cubeball.manage")) {
                        if (match != null) {
                            match.start(player);
                        } else {
                            player.sendMessage("[Cubeball] " + ChatColor.RED + "You need te create a match before (/cb match) !");
                        }
                    } else if (args[0].equalsIgnoreCase("stop") && player.hasPermission("cubeball.manage")) {
                        if (match != null) {
                            match.stop();
                            match = null;
                            player.sendMessage("[Cubeball] " + ChatColor.GREEN + "Match cancelled ! To create a new match do /cb match");
                        } else {
                            player.sendMessage("[Cubeball] " + ChatColor.RED + "No match to stop");
                        }
                    } else if (args[0].equalsIgnoreCase("pause") && player.hasPermission("cubeball.manage")) {
                        if (match != null && match.pause()) {
                            for (Player p : match.getAllPlayer()) {
                                if (p != null) {
                                    p.sendMessage("[Cubeball] " + ChatColor.RED + "Match PAUSED by " + ChatColor.GOLD + player.getName());
                                }
                            }
                        } else {
                            player.sendMessage("[Cubeball] " + ChatColor.RED + "No match to pause");
                        }
                    } else if (args[0].equalsIgnoreCase("resume") && player.hasPermission("cubeball.manage")) {
                        if (match != null && match.resume()) {
                            for (Player p : match.getAllPlayer()) {
                                if (p != null) {
                                    p.sendMessage("[Cubeball] " + ChatColor.RED + "Match RESUMED by " + ChatColor.GOLD + player.getName());
                                }
                            }
                        } else {
                            player.sendMessage("[Cubeball] " + ChatColor.RED + "No match to resume");
                        }
                    } else {
                        player.sendMessage("[Cubeball] " + ChatColor.RED + "Unknown command");
                    }
                } else if (args.length == 2) {
                    if (args[0].equalsIgnoreCase("generate") && player.hasPermission("cubeball.manage")) {
                        if (balls.get(args[1]) == null) {
                            generateBall(args[1], player.getLocation(), null);
                            player.sendMessage("[Cubeball] " + ChatColor.GREEN + "Ball generated with ID : " + args[1]);
                        } else {
                            player.sendMessage("[Cubeball] " + ChatColor.RED + "Ball already exists with ID : " + args[1]);
                        }
                    } else if (args[0].equalsIgnoreCase("remove") && player.hasPermission("cubeball.manage")) {
                        if (balls.get(args[1]) != null) {
                            destroyBall(args[1]);
                            player.sendMessage("[Cubeball] " + ChatColor.GREEN + "Ball with ID " + args[1] + " removed !");
                        } else {
                            sender.sendMessage("[Cubeball] " + ChatColor.RED + "Ball with ID " + args[1] + " doesn't exists !");
                        }

                    }
                }
                if (args.length == 3) {
                    if (args[0].equalsIgnoreCase("team") && player.hasPermission("cubeball.manage")) {
                        if (match != null && match.isClubMatch()) {
                            player.sendMessage("[CubeBall] /cb team is only available for free matches."); return true;
                        }
                        if (match != null) {
                            try {
                                Team team = Team.valueOf(args[1].toUpperCase());
                                Player playerToAdd = Bukkit.getPlayer(args[2]);
                                if (match.addPlayerToTeam(playerToAdd, team)) {
                                    player.sendMessage("[Cubeball] " + ChatColor.GREEN + "Player added to " + args[1].toUpperCase() + " team !");
                                    playerToAdd.sendMessage("[Cubeball] " + ChatColor.GREEN + "Your are in the " + args[1].toUpperCase() + " team !");
                                    match.displayTeams(player);
                                    player.sendMessage("use /cb start when you are ready");
                                } else {
                                    player.sendMessage("[Cubeball] " + ChatColor.RED + "Cannot find this player !");
                                }
                            } catch (IllegalArgumentException | NullPointerException e) {
                                player.sendMessage("[Cubeball] " + ChatColor.RED + "Team must be RED or BLUE or SPECTATOR");
                            }
                        } else {
                            player.sendMessage("[Cubeball] " + ChatColor.RED + "You need te create a match before (/cb match) !");
                        }
                    }
                }
                if (args.length == 6) {
                    if (args[0].equalsIgnoreCase("generate") && player.hasPermission("cubeball.manage")) {
                        generateWithPosition(sender, args);
                    }
                }
            }

        } else {
            if (args.length == 1) {
                if (args[0].equalsIgnoreCase("reload")) {
                    if (hasManagePermission(sender)) {
                        reloadPluginConfiguration();
                        sender.sendMessage("[Cubeball] " + ChatColor.GREEN + "Configuration reloaded.");
                    } else {
                        sender.sendMessage("[Cubeball] " + ChatColor.RED + "You do not have permission to do that.");
                    }
                }
            }

            if (args.length == 2) {
                if (args[0].equalsIgnoreCase("remove")) {
                    if (balls.get(args[1]) != null) {
                        destroyBall(args[1]);
                        sender.sendMessage("[Cubeball] " + ChatColor.GREEN + "Ball with ID " + args[1] + " removed !");
                    } else {
                        sender.sendMessage("[Cubeball] " + ChatColor.RED + "Ball with ID " + args[1] + " doesn't exists !");
                    }
                }

            }

            if (args.length == 6) {
                if (args[0].equalsIgnoreCase("generate")) {
                    generateWithPosition(sender, args);
                }
            }
        }
        return true;
    }

    private boolean hasManagePermission(CommandSender sender) {
        return !(sender instanceof Player) || sender.isOp() || sender.hasPermission("cubeball.manage");
    }

    private static void generateWithPosition(CommandSender sender, String[] args) {
        if (balls.get(args[1]) == null) {

            if (Bukkit.getWorld(args[5]) != null) {
                generateBall(args[1],
                        new Location(Bukkit.getWorld(args[5]),
                                Double.parseDouble(args[2]),
                                Double.parseDouble(args[3]),
                                Double.parseDouble(args[4])
                        ), null);
                sender.sendMessage("[Cubeball] " + ChatColor.GREEN + "Ball generated with ID : " + args[1] + "at (x:" + args[2] + " y:" + args[3] + " z:" + args[4] + ") on " + args[5]);
            } else {
                sender.sendMessage("[Cubeball] " + ChatColor.RED + "Unknown world :" + args[5]);
            }
        } else {
            sender.sendMessage("[Cubeball] " + ChatColor.RED + "Ball already exists with ID : " + args[1]);
        }
    }
}
