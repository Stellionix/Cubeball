package me.crylonz;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.*;
import static me.crylonz.CubeBall.*;

final class ClubCommands {
    static boolean admin(CommandSender sender) {
        return !(sender instanceof Player) || sender.isOp() || sender.hasPermission("cubeball.manage");
    }
    static boolean handle(CommandSender sender, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) { help(sender); return true; }
        String root = args[0].toLowerCase(Locale.ROOT);
        if (!root.equals("stats") && !root.equals("club") && !(root.equals("match") && args.length > 1)) return false;
        try {
            if (clubStore == null) throw new IllegalStateException("Persistent features are unavailable.");
            clubStore.requireAvailable();
            if (root.equals("stats")) {
                if (args.length > 2 || (args.length == 1 && !(sender instanceof Player)))
                    throw new IllegalArgumentException("Usage: /cb stats <player>");
                UUID id;
                if (args.length == 2) id = resolvePlayer(args[1]);
                else { Player p = (Player) sender; clubStore.remember(p.getUniqueId(), p.getName()); id = p.getUniqueId(); }
                ClubStore.Profile p = clubStore.profile(id);
                String club = clubStore.membership(id);
                sender.sendMessage("[CubeBall] " + p.name() + " | Goals: " + p.goals() + " | Assists: " + p.assists()
                        + " | Club: " + (club == null ? "None" : clubStore.club(club).name()));
            } else if (root.equals("match")) {
                requireAdmin(sender);
                if (!(sender instanceof Player)) throw new IllegalArgumentException("Prepare the arena in game.");
                arity(args, 3, "/cb match <blueClub> <redClub>");
                Match next = new Match(args[1], args[2]);
                if (match != null) match.stop();
                match = next; match.scanSpawn((Player) sender);
            } else club(sender, args);
        } catch (IllegalArgumentException | IllegalStateException e) { sender.sendMessage("[CubeBall] " + e.getMessage()); }
        return true;
    }
    private static void club(CommandSender sender, String[] a) {
        if (a.length < 2) { help(sender); return; }
        String action = a[1].toLowerCase(Locale.ROOT);
        if (Set.of("create", "delete", "manager", "add", "remove").contains(action)) requireAdmin(sender);
        switch (action) {
            case "list" -> {
                arity(a, 2, "/cb club list");
                sender.sendMessage("[CubeBall] Clubs: " + clubStore.clubs().size());
                clubStore.clubs().forEach(c -> sender.sendMessage(c.id() + " — " + c.name()));
            }
            case "info" -> {
                arity(a, 3, "/cb club info <id>"); ClubStore.Club c = clubStore.club(a[2]);
                sender.sendMessage(c.name() + " (" + c.id() + ") | Manager: " + name(c.manager()));
                sender.sendMessage("Members: " + String.join(", ", clubStore.members(c.id()).stream().map(ClubCommands::name).toList()));
            }
            case "create" -> {
                if (a.length < 4) throw new IllegalArgumentException("Usage: /cb club create <id> <name...>");
                clubStore.create(a[2], String.join(" ", Arrays.copyOfRange(a, 3, a.length)));
                sender.sendMessage("[CubeBall] Club created.");
            }
            case "delete" -> { arity(a, 3, "/cb club delete <id>"); clubStore.delete(a[2]); sender.sendMessage("[CubeBall] Club deleted."); }
            case "manager", "add", "remove" -> {
                arity(a, 4, "/cb club " + action + " <id> <player>"); UUID id = resolvePlayer(a[3]);
                switch (action) {
                    case "manager" -> clubStore.manager(a[2], id);
                    case "add" -> clubStore.add(a[2], id);
                    default -> clubStore.remove(a[2], id);
                }
                sender.sendMessage("[CubeBall] Club updated.");
            }
            case "transfer" -> {
                arity(a, 4, "/cb club transfer <player> <destination>");
                ClubStore.Transfer t = clubStore.propose(actor(sender), resolvePlayer(a[2]), a[3]);
                sender.sendMessage("[CubeBall] Transfer proposed: " + t.id());
            }
            case "transfers" -> {
                arity(a, 2, "/cb club transfers");
                UUID actor = sender instanceof Player p ? p.getUniqueId() : null;
                List<ClubStore.Transfer> pending = clubStore.transfers().stream()
                        .filter(t -> actor == null || actor.equals(t.sourceManager()) || actor.equals(t.destinationManager())).toList();
                sender.sendMessage("[CubeBall] Pending transfers: " + pending.size());
                pending.forEach(t -> sender.sendMessage(t.id() + " | " + name(t.player()) + " | " + t.source() + " -> " + t.destination()));
            }
            case "accept", "reject", "cancel" -> {
                arity(a, 3, "/cb club " + action + " <transfer>"); clubStore.resolve(a[2], actor(sender), action);
                sender.sendMessage("[CubeBall] Transfer " + action + " completed.");
            }
            default -> help(sender);
        }
    }
    static UUID resolvePlayer(String input) {
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) { clubStore.remember(online.getUniqueId(), online.getName()); return online.getUniqueId(); }
        // getOfflinePlayers only uses known server records; never invent an offline UUID from a name.
        for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
            if (p.getName() != null && (p.getName().equalsIgnoreCase(input) || p.getUniqueId().toString().equalsIgnoreCase(input))) {
                clubStore.remember(p.getUniqueId(), p.getName()); return p.getUniqueId();
            }
        }
        return clubStore.profiles().stream().filter(p -> p.name().equalsIgnoreCase(input) || p.id().toString().equalsIgnoreCase(input))
                .map(ClubStore.Profile::id).findFirst().orElseThrow(() -> new IllegalArgumentException("Unknown player: " + input));
    }
    private static String name(UUID id) {
        if (id == null) return "None";
        ClubStore.Profile p = clubStore.profile(id); return p == null ? id.toString() : p.name();
    }
    private static UUID actor(CommandSender sender) {
        if (sender instanceof Player p) return p.getUniqueId();
        throw new IllegalArgumentException("Transfer decisions require the club manager to be in game.");
    }
    private static void arity(String[] a, int count, String usage) {
        if (a.length != count) throw new IllegalArgumentException("Usage: " + usage);
    }
    private static void requireAdmin(CommandSender sender) {
        if (!admin(sender)) throw new IllegalArgumentException("You do not have permission to do that.");
    }
    static void help(CommandSender sender) {
        sender.sendMessage("/cb stats [player] | /cb club list | /cb club info <id>");
        if (sender instanceof Player p && clubStore != null && clubStore.available()
                && clubStore.clubs().stream().anyMatch(c -> p.getUniqueId().equals(c.manager()))) {
            sender.sendMessage("/cb club transfer <player> <destination> | /cb club transfers");
            sender.sendMessage("/cb club accept|reject|cancel <transfer>");
        }
        if (admin(sender)) {
            sender.sendMessage("/cb club create <id> <name...> | /cb club delete <id>");
            sender.sendMessage("/cb club manager|add|remove <id> <player> | /cb club transfers");
            sender.sendMessage("/cb match [blueClub redClub] | /cb team BLUE|RED|SPECTATOR <player>");
            sender.sendMessage("/cb start|stop|pause|resume|reload | /cb generate <id> [x y z world] | /cb remove <id>");
        }
    }
}
