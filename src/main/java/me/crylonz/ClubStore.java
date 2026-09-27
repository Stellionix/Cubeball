package me.crylonz;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;

/** Persistent domain service. No command senders, players or match objects are stored here. */
public final class ClubStore {
    public record Profile(UUID id, String name, long goals, long assists) {}
    public record Club(String id, String name, UUID manager) {}
    public record Transfer(String id, UUID player, String source, String destination,
                           UUID sourceManager, UUID destinationManager) {}

    private final Path file;
    private final Consumer<String> errors;
    private final Map<UUID, Profile> profiles = new LinkedHashMap<>();
    private final Map<String, Club> clubs = new LinkedHashMap<>();
    private final Map<UUID, String> memberships = new LinkedHashMap<>();
    private final Map<String, Transfer> transfers = new LinkedHashMap<>();
    private boolean available = true;
    private int maxMembers;

    public ClubStore(Path file, int maxMembers, Consumer<String> errors) {
        this.file = file;
        this.maxMembers = maxMembers;
        this.errors = errors;
        if (Files.exists(file)) {
            try { load(); }
            catch (Exception e) { disable(e); }
        }
    }

    public boolean available() { return available; }
    public void setMaxMembers(int value) { maxMembers = Math.max(0, value); }
    public void requireAvailable() {
        if (!available) throw new IllegalStateException("Persistent features are unavailable. Check the server log.");
    }
    public Collection<Profile> profiles() { requireAvailable(); return List.copyOf(profiles.values()); }
    public Collection<Club> clubs() { requireAvailable(); return List.copyOf(clubs.values()); }
    public Collection<Transfer> transfers() { requireAvailable(); return List.copyOf(transfers.values()); }
    public Profile profile(UUID id) { requireAvailable(); return profiles.get(id); }
    public String membership(UUID player) { requireAvailable(); return memberships.get(player); }
    public Set<UUID> members(String club) {
        requireAvailable();
        Set<UUID> result = new LinkedHashSet<>();
        memberships.forEach((id, value) -> { if (value.equals(club)) result.add(id); });
        return Collections.unmodifiableSet(result);
    }
    public Club club(String id) {
        requireAvailable();
        Club c = clubs.get(normalize(id));
        check(c != null, "Unknown club: " + id);
        return c;
    }
    public void remember(UUID id, String name) {
        requireAvailable();
        Objects.requireNonNull(id);
        check(name != null && !name.isBlank(), "Player name is required.");
        Profile old = profiles.get(id);
        if (old != null && old.name().equals(name)) return;
        profiles.put(id, new Profile(id, name, old == null ? 0 : old.goals(), old == null ? 0 : old.assists()));
        save();
    }
    public void recordGoal(UUID scorer, UUID assistant) {
        requireAvailable();
        if (scorer == null) return;
        check(profiles.containsKey(scorer), "Unknown scorer.");
        check(assistant == null || profiles.containsKey(assistant), "Unknown assistant.");
        Profile p = profiles.get(scorer);
        profiles.put(scorer, new Profile(scorer, p.name(), p.goals() + 1, p.assists()));
        if (assistant != null && !assistant.equals(scorer)) {
            p = profiles.get(assistant);
            profiles.put(assistant, new Profile(assistant, p.name(), p.goals(), p.assists() + 1));
        }
        save();
    }
    public void create(String id, String name) {
        requireAvailable(); id = normalize(id);
        check(id.matches("[a-z0-9_-]+"), "Club IDs must use letters, digits, _ or -.");
        check(!clubs.containsKey(id), "Club already exists.");
        check(!name.isBlank(), "Club name is required.");
        clubs.put(id, new Club(id, name, null)); save();
    }
    public void delete(String id) {
        id = club(id).id(); final String target = id;
        invalidateClub(id); memberships.values().removeIf(target::equals); clubs.remove(id); save();
    }
    public void manager(String id, UUID manager) {
        Club c = club(id); check(profiles.containsKey(manager), "Unknown player.");
        invalidateClub(c.id()); clubs.put(c.id(), new Club(c.id(), c.name(), manager)); save();
    }
    public void add(String id, UUID player) {
        Club c = club(id); check(profiles.containsKey(player), "Unknown player.");
        check(!memberships.containsKey(player), "Player already belongs to a club.");
        capacity(c.id()); invalidatePlayer(player); memberships.put(player, c.id()); save();
    }
    public void remove(String id, UUID player) {
        Club c = club(id); check(c.id().equals(memberships.get(player)), "Player is not a member of this club.");
        invalidatePlayer(player); memberships.remove(player); save();
    }
    public Transfer propose(UUID manager, UUID player, String destination) {
        requireAvailable(); String source = memberships.get(player);
        check(source != null, "Player has no club.");
        Club from = club(source), to = club(destination);
        check(manager != null && manager.equals(from.manager()), "Only the source club manager can propose a transfer.");
        check(!from.id().equals(to.id()), "Choose a different club.");
        check(to.manager() != null, "Destination club needs a manager.");
        check(transfers.values().stream().noneMatch(t -> t.player().equals(player)), "Player already has a pending transfer.");
        capacity(to.id());
        Transfer t = new Transfer(UUID.randomUUID().toString(), player, from.id(), to.id(), manager, to.manager());
        transfers.put(t.id(), t); save(); return t;
    }
    public void resolve(String id, UUID actor, String action) {
        requireAvailable(); Transfer t = transfers.get(id);
        check(t != null, "Unknown or expired transfer.");
        check(Set.of("accept", "reject", "cancel").contains(action), "Unknown transfer action.");
        UUID required = action.equals("cancel") ? t.sourceManager() : t.destinationManager();
        check(actor != null && actor.equals(required), "Only the relevant club manager can do that.");
        if (!valid(t)) { transfers.remove(id); save(); throw new IllegalArgumentException("Transfer has expired."); }
        if (action.equals("accept")) {
            capacity(t.destination()); memberships.put(t.player(), t.destination()); invalidatePlayer(t.player());
        } else transfers.remove(id);
        save();
    }
    private boolean valid(Transfer t) {
        Club from = clubs.get(t.source()), to = clubs.get(t.destination());
        return from != null && to != null && t.source().equals(memberships.get(t.player()))
                && t.sourceManager().equals(from.manager()) && t.destinationManager().equals(to.manager());
    }
    private void capacity(String id) { check(maxMembers <= 0 || members(id).size() < maxMembers, "Club is full."); }
    private void invalidatePlayer(UUID player) { transfers.values().removeIf(t -> t.player().equals(player)); }
    private void invalidateClub(String id) { transfers.values().removeIf(t -> t.source().equals(id) || t.destination().equals(id)); }
    private static String normalize(String id) { return id.toLowerCase(Locale.ROOT); }
    private static void check(boolean condition, String message) { if (!condition) throw new IllegalArgumentException(message); }
    private void disable(Exception e) { available = false; errors.accept("CubeBall persistent features disabled; data file preserved: " + e); }

    private void save() {
        requireAvailable();
        Path temporary = null;
        try {
            YamlConfiguration yaml = new YamlConfiguration(); yaml.set("version", 1);
            yaml.createSection("profiles"); yaml.createSection("clubs"); yaml.createSection("transfers");
            for (Profile p : profiles.values()) {
                String key = "profiles." + p.id();
                yaml.set(key + ".name", p.name()); yaml.set(key + ".goals", p.goals()); yaml.set(key + ".assists", p.assists());
                yaml.set(key + ".club", memberships.get(p.id()));
            }
            for (Club c : clubs.values()) {
                String key = "clubs." + c.id(); yaml.set(key + ".name", c.name());
                yaml.set(key + ".manager", c.manager() == null ? null : c.manager().toString());
            }
            for (Transfer t : transfers.values()) {
                String key = "transfers." + t.id(); yaml.set(key + ".player", t.player().toString());
                yaml.set(key + ".source", t.source()); yaml.set(key + ".destination", t.destination());
                yaml.set(key + ".source-manager", t.sourceManager().toString());
                yaml.set(key + ".destination-manager", t.destinationManager().toString());
            }
            Files.createDirectories(file.toAbsolutePath().getParent());
            temporary = Files.createTempFile(file.toAbsolutePath().getParent(), "cubeball-", ".tmp");
            Files.writeString(temporary, yaml.saveToString(), StandardCharsets.UTF_8);
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) { disable(e); throw new IllegalStateException("Could not save persistent data. Check the server log.", e); }
        finally { if (temporary != null) try { Files.deleteIfExists(temporary); } catch (Exception ignored) { } }
    }
    private void load() throws Exception {
        YamlConfiguration y = new YamlConfiguration(); y.load(file.toFile());
        check(y.isInt("version") && y.getInt("version") == 1, "Unsupported data version.");
        ConfigurationSection ps = section(y, "profiles"), cs = section(y, "clubs"), ts = section(y, "transfers");
        for (String key : ps.getKeys(false)) {
            ConfigurationSection p = section(ps, key); UUID id = UUID.fromString(key);
            String name = required(p, "name");
            check(p.isLong("goals") || p.isInt("goals"), "Invalid goals.");
            check(p.isLong("assists") || p.isInt("assists"), "Invalid assists.");
            long goals = p.getLong("goals"), assists = p.getLong("assists");
            check(goals >= 0 && assists >= 0, "Negative statistics.");
            profiles.put(id, new Profile(id, name, goals, assists));
            if (p.contains("club")) memberships.put(id, required(p, "club"));
        }
        for (String key : cs.getKeys(false)) {
            check(key.matches("[a-z0-9_-]+"), "Invalid club ID."); ConfigurationSection c = section(cs, key);
            UUID manager = c.contains("manager") ? UUID.fromString(required(c, "manager")) : null;
            check(manager == null || profiles.containsKey(manager), "Unknown manager.");
            clubs.put(key, new Club(key, required(c, "name"), manager));
        }
        check(memberships.values().stream().allMatch(clubs::containsKey), "Unknown membership club.");
        Set<UUID> pending = new HashSet<>();
        for (String key : ts.getKeys(false)) {
            UUID.fromString(key); ConfigurationSection t = section(ts, key);
            Transfer transfer = new Transfer(key, UUID.fromString(required(t, "player")), required(t, "source"), required(t, "destination"),
                    UUID.fromString(required(t, "source-manager")), UUID.fromString(required(t, "destination-manager")));
            check(valid(transfer) && !transfer.source().equals(transfer.destination()) && pending.add(transfer.player()), "Invalid pending transfer.");
            transfers.put(key, transfer);
        }
    }
    private static ConfigurationSection section(ConfigurationSection parent, String path) {
        ConfigurationSection s = parent.getConfigurationSection(path); check(s != null, "Missing section: " + path); return s;
    }
    private static String required(ConfigurationSection parent, String path) {
        check(parent.isString(path), "Invalid field: " + path); String s = parent.getString(path);
        check(s != null && !s.isBlank(), "Empty field: " + path); return s;
    }
}
