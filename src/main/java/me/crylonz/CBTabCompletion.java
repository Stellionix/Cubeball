package me.crylonz;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;
import static me.crylonz.CubeBall.*;

public class CBTabCompletion implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("cb") || args.length == 0) return List.of();
        List<String> options = new ArrayList<>();
        boolean admin = ClubCommands.admin(sender);
        UUID actor = sender instanceof Player p ? p.getUniqueId() : null;
        boolean available = clubStore != null && clubStore.available();
        boolean manager = available && actor != null && clubStore.clubs().stream().anyMatch(c -> actor.equals(c.manager()));
        String root = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            if (admin) options.addAll(List.of("reload", "generate", "remove", "match", "start", "stop", "pause", "resume"));
            if (admin && (match == null || !match.isClubMatch())) options.add("team");
            options.addAll(List.of("stats", "club", "help"));
        } else if (root.equals("stats") && args.length == 2) names(options);
        else if (root.equals("club") && available) {
            String action = args[1].toLowerCase(Locale.ROOT);
            if (args.length == 2) {
                options.addAll(List.of("list", "info"));
                if (admin) options.addAll(List.of("create", "delete", "manager", "add", "remove"));
                if (manager || actor == null) options.add("transfers");
                if (manager) options.addAll(List.of("transfer", "accept", "reject", "cancel"));
            } else if (args.length == 3) {
                if (action.equals("info") || (admin && Set.of("delete", "manager", "add", "remove").contains(action)))
                    clubStore.clubs().forEach(c -> options.add(c.id()));
                if (manager && action.equals("transfer"))
                    clubStore.clubs().stream().filter(c -> actor.equals(c.manager())).forEach(c ->
                        clubStore.members(c.id()).forEach(id -> options.add(clubStore.profile(id).name())));
                if (manager && Set.of("accept", "reject", "cancel").contains(action))
                    clubStore.transfers().stream().filter(t -> actor.equals(action.equals("cancel") ? t.sourceManager() : t.destinationManager()))
                            .forEach(t -> options.add(t.id()));
            } else if (args.length == 4) {
                if (admin && Set.of("manager", "add", "remove").contains(action)) names(options);
                if (manager && action.equals("transfer")) clubStore.clubs().forEach(c -> options.add(c.id()));
            }
        } else if (admin) {
            if (root.equals("match") && available && (args.length == 2 || args.length == 3))
                clubStore.clubs().forEach(c -> { if (args.length == 2 || !c.id().equalsIgnoreCase(args[1])) options.add(c.id()); });
            if (root.equals("team") && (match == null || !match.isClubMatch())) {
                if (args.length == 2) options.addAll(List.of("BLUE", "RED", "SPECTATOR"));
                if (args.length == 3) Bukkit.getOnlinePlayers().forEach(p -> options.add(p.getName()));
            }
            if (root.equals("remove") && args.length == 2) balls.keySet().stream().filter(id -> !id.equals(BALL_MATCH_ID)).forEach(options::add);
            if (root.equals("generate")) {
                if (args.length >= 2 && args.length <= 5) options.add(List.of("<ID>", "<x>", "<y>", "<z>").get(args.length - 2));
                if (args.length == 6) Bukkit.getWorlds().forEach(w -> options.add(w.getName()));
            }
        }
        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().distinct().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
    private static void names(List<String> result) {
        Bukkit.getOnlinePlayers().forEach(p -> result.add(p.getName()));
        for (org.bukkit.OfflinePlayer p : Bukkit.getOfflinePlayers()) if (p.getName() != null) result.add(p.getName());
        if (clubStore != null && clubStore.available()) clubStore.profiles().forEach(p -> result.add(p.name()));
    }
}
