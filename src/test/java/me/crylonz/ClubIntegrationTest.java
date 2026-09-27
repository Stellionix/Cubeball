package me.crylonz;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Logger;
import static me.crylonz.CubeBall.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ClubIntegrationTest {
    static Server server;
    @TempDir Path directory;
    BukkitScheduler scheduler;
    List<Runnable> delayed;
    Player alice, bob, carol;
    Command command;
    World world;
    @BeforeAll static void server() {
        server = mock(Server.class);
        when(server.getLogger()).thenReturn(Logger.getLogger("CubeBallTest"));
        when(server.getName()).thenReturn("Test"); when(server.getVersion()).thenReturn("Test");
        when(server.getBukkitVersion()).thenReturn("Test"); Bukkit.setServer(server);
    }
    @BeforeEach void setup() {
        reset(server); scheduler = mock(BukkitScheduler.class); delayed = new ArrayList<>();
        when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.scheduleSyncDelayedTask(any(Plugin.class), any(Runnable.class), anyLong())).thenAnswer(i -> {
            delayed.add(i.getArgument(1)); return delayed.size();
        });
        plugin = mock(Plugin.class); balls.clear(); match = null; maxGoal = 0; spawnGoalFireworks = false;
        clubStore = new ClubStore(directory.resolve("clubs.yml"), 0, s -> fail(s));
        alice = player("Alice"); bob = player("Bob"); carol = player("Carol");
        when(server.getOnlinePlayers()).thenAnswer(i -> List.of(alice, bob, carol));
        when(server.getOfflinePlayers()).thenReturn(new OfflinePlayer[0]);
        clubStore.create("blue", "Blue Club"); clubStore.create("red", "Red Club");
        clubStore.manager("blue", alice.getUniqueId()); clubStore.manager("red", bob.getUniqueId());
        clubStore.add("blue", alice.getUniqueId()); clubStore.add("red", bob.getUniqueId());
        command = mock(Command.class); when(command.getName()).thenReturn("cb"); world = mock(World.class);
        org.bukkit.block.Block goalBlock = mock(org.bukkit.block.Block.class);
        when(world.getBlockAt(any(Location.class))).thenReturn(goalBlock);
        when(goalBlock.getLocation()).thenReturn(new Location(world, 20, 0, 0));
    }
    @AfterEach void cleanup() { clubStore = null; match = null; balls.clear(); }
    Player player(String name) {
        Player p = mock(Player.class); when(p.getUniqueId()).thenReturn(UUID.randomUUID()); when(p.getName()).thenReturn(name);
        clubStore.remember(p.getUniqueId(), name); return p;
    }
    void field(Match m, String name, Object value) throws Exception {
        Field f = Match.class.getDeclaredField(name); f.setAccessible(true); f.set(m, value);
    }
    Match ready(boolean clubs) throws Exception {
        Match m = clubs ? new Match("blue", "red") : new Match();
        if (!clubs) { m.addPlayerToTeam(alice, Team.BLUE); m.addPlayerToTeam(carol, Team.BLUE); m.addPlayerToTeam(bob, Team.RED); }
        field(m, "blueTeamSpawns", new ArrayList<>(List.of(new Location(world, 0, 0, 0))));
        field(m, "redTeamSpawns", new ArrayList<>(List.of(new Location(world, 10, 0, 0))));
        field(m, "redTeamGoalBlocks", new ArrayList<>(List.of(new Location(world, 20, 0, 0))));
        field(m, "ballSpawn", new Location(world, 5, 0, 0));
        m.setMatchState(MatchState.READY); return m;
    }
    void scoreBlue(Match m) { m.checkGoal(new Location(world, 20, 1, 0)); }

    @Test void normalAndOvertimeGoalsPersistExactlyOnce() throws Exception {
        Match m = ready(false); m.setMatchState(MatchState.IN_PROGRESS);
        m.touch(BALL_MATCH_ID, alice); m.touch(BALL_MATCH_ID, carol); m.touch(BALL_MATCH_ID, carol);
        scoreBlue(m); scoreBlue(m);
        assertEquals(1, m.getBlueScore()); assertEquals(1, clubStore.profile(carol.getUniqueId()).goals());
        assertEquals(1, clubStore.profile(alice.getUniqueId()).assists());
        m.setMatchState(MatchState.OVERTIME); m.touch(BALL_MATCH_ID, alice); scoreBlue(m);
        assertEquals(MatchState.END, m.getMatchState()); assertEquals(1, clubStore.profile(alice.getUniqueId()).goals());
        assertEquals(0, clubStore.profile(carol.getUniqueId()).assists());
    }
    @Test void freeBallsSpectatorsOwnGoalsAndNoAuthorDoNotAwardStats() throws Exception {
        Match m = ready(false); m.setMatchState(MatchState.IN_PROGRESS);
        Player spectator = player("Spectator");
        assertFalse(m.touch(BALL_MATCH_ID, spectator)); assertFalse(m.touch("training", alice));
        scoreBlue(m); assertEquals(1, m.getBlueScore());
        m.setMatchState(MatchState.IN_PROGRESS); m.touch(BALL_MATCH_ID, bob); scoreBlue(m);
        assertEquals(2, m.getBlueScore()); assertEquals(0, clubStore.profile(bob.getUniqueId()).goals());
        assertEquals(0, clubStore.profile(alice.getUniqueId()).goals());
    }
    @Test void pauseClearsContactsAndStopNeutralizesAlreadyQueuedCallbacks() throws Exception {
        Match m = ready(false); m.start(alice); m.touch(BALL_MATCH_ID, alice);
        assertTrue(m.pause()); assertTrue(m.resume()); assertFalse(m.resume());
        scoreBlue(m); assertEquals(0, clubStore.profile(alice.getUniqueId()).goals());
        Ball ball = new Ball(); var entity = mock(org.bukkit.entity.FallingBlock.class); ball.setBall(entity); balls.put(BALL_MATCH_ID, ball);
        m.stop(); verify(entity).remove(); assertFalse(balls.containsKey(BALL_MATCH_ID));
        for (Runnable callback : List.copyOf(delayed)) callback.run();
        assertEquals(MatchState.END, m.getMatchState()); assertFalse(balls.containsKey(BALL_MATCH_ID));
        verify(scheduler, atLeastOnce()).cancelTask(anyInt());
    }
    @Test void refreshThenFreezeUnequalRostersAndReconnectAfterTransfer() throws Exception {
        Match m = ready(true); clubStore.add("blue", carol.getUniqueId()); m.start(alice);
        assertEquals(2, m.getBlueTeam().size()); assertEquals(1, m.getRedTeam().size());
        assertFalse(m.addPlayerToTeam(carol, Team.RED));
        var t = clubStore.propose(alice.getUniqueId(), carol.getUniqueId(), "red"); clubStore.resolve(t.id(), bob.getUniqueId(), "accept");
        assertEquals(Team.BLUE, m.teamOf(carol));
        UUID carolId = carol.getUniqueId();
        Player reconnected = mock(Player.class); when(reconnected.getUniqueId()).thenReturn(carolId); m.reconnect(reconnected);
        assertTrue(m.getBlueTeam().contains(reconnected)); assertFalse(m.getBlueTeam().contains(carol));
        assertEquals(Team.RED, new Match("blue", "red").teamOf(carol));
        Player newcomer = player("Newcomer"); clubStore.add("blue", newcomer.getUniqueId()); m.reconnect(newcomer);
        assertEquals(Team.SPECTATOR, m.teamOf(newcomer));
    }
    @Test void clubStartRequiresConnectedMembersOnBothSides() throws Exception {
        Match m = ready(true); when(server.getOnlinePlayers()).thenAnswer(i -> List.of(alice)); m.start(alice);
        assertEquals(MatchState.READY, m.getMatchState()); assertTrue(delayed.isEmpty());
    }
    @Test void commandsRespectRightsAndConsoleCanLookUpKnownOfflinePlayers() {
        CBCommandExecutor executor = new CBCommandExecutor();
        executor.onCommand(carol, command, "cb", new String[]{"club", "create", "other", "Other"});
        assertEquals(2, clubStore.clubs().size());
        verify(carol).sendMessage(contains("permission"));
        CommandSender console = mock(CommandSender.class);
        executor.onCommand(console, command, "cb", new String[]{"club", "create", "other", "Other", "Club"});
        assertEquals("Other Club", clubStore.club("other").name());
        OfflinePlayer offline = mock(OfflinePlayer.class); UUID id = UUID.randomUUID();
        when(offline.getName()).thenReturn("Offline"); when(offline.getUniqueId()).thenReturn(id);
        when(server.getOfflinePlayers()).thenReturn(new OfflinePlayer[]{offline});
        executor.onCommand(console, command, "cb", new String[]{"stats", "Offline"});
        assertNotNull(clubStore.profile(id)); verify(console).sendMessage(contains("Goals: 0"));
        executor.onCommand(console, command, "cb", new String[]{"club", "add", "other", "Offline"});
        assertEquals("other", clubStore.membership(id));
        executor.onCommand(console, command, "cb", new String[]{"match", "blue", "red"});
        verify(console).sendMessage(contains("in game")); assertNull(match);
    }
    @Test void managerCommandsAndCompletionDoNotRequireAdminPermission() {
        CBCommandExecutor executor = new CBCommandExecutor();
        executor.onCommand(alice, command, "cb", new String[]{"club", "transfer", "Alice", "red"});
        var t = clubStore.transfers().iterator().next();
        CBTabCompletion tabs = new CBTabCompletion();
        assertTrue(tabs.onTabComplete(bob, command, "cb", new String[]{"club", "accept", ""}).contains(t.id()));
        assertFalse(tabs.onTabComplete(carol, command, "cb", new String[]{"club", ""}).contains("create"));
        executor.onCommand(carol, command, "cb", new String[]{"club", "accept", t.id()});
        assertEquals("blue", clubStore.membership(alice.getUniqueId()));
        executor.onCommand(bob, command, "cb", new String[]{"club", "accept", t.id()});
        assertEquals("red", clubStore.membership(alice.getUniqueId()));
        executor.onCommand(bob, command, "cb", new String[]{"club", "accept", t.id()});
        verify(bob).sendMessage(contains("expired transfer"));
    }

    @Test void physicalBouncePreservesGoalAndAssistContacts() throws Exception {
        match = ready(false); match.setMatchState(MatchState.IN_PROGRESS);
        match.touch(BALL_MATCH_ID, alice); match.touch(BALL_MATCH_ID, carol);
        var original = mock(org.bukkit.entity.FallingBlock.class);
        var replacement = mock(org.bukkit.entity.FallingBlock.class);
        var blockData = mock(org.bukkit.block.data.BlockData.class);
        Location location = new Location(world, 5, 0, 0);
        when(original.getVelocity()).thenReturn(new org.bukkit.util.Vector(1, 0, 0));
        when(original.getLocation()).thenReturn(location);
        when(replacement.getWorld()).thenReturn(world); when(replacement.getLocation()).thenReturn(location);
        when(server.createBlockData(cubeBallBlock)).thenReturn(blockData);
        when(world.spawnFallingBlock(location, blockData)).thenReturn(replacement);
        Ball ball = new Ball(); ball.setId(BALL_MATCH_ID); ball.setBall(original); balls.put(BALL_MATCH_ID, ball);
        var event = mock(org.bukkit.event.entity.EntityChangeBlockEvent.class);
        when(event.getTo()).thenReturn(cubeBallBlock); when(event.getEntityType()).thenReturn(org.bukkit.entity.EntityType.FALLING_BLOCK);
        when(event.getEntity()).thenReturn(original);
        new CubeBallListener().blockChangeEvent(event);
        verify(event).setCancelled(true); verify(original).remove();
        assertSame(replacement, balls.get(BALL_MATCH_ID).getBall());
        scoreBlue(match);
        assertEquals(1, clubStore.profile(carol.getUniqueId()).goals());
        assertEquals(1, clubStore.profile(alice.getUniqueId()).assists());
    }

    @Test void replacementCommandCancelsPreviousCountdown() throws Exception {
        match = ready(false); Match previous = match; previous.start(alice);
        when(alice.hasPermission("cubeball.manage")).thenReturn(true);
        Location position = new Location(world, 0, 0, 0); when(alice.getLocation()).thenReturn(position);
        var block = world.getBlockAt(position);
        when(block.getRelative(anyInt(), anyInt(), anyInt())).thenReturn(block);
        when(block.getType()).thenReturn(Material.STONE);
        int radius = scanRadius; scanRadius = 0;
        try { new CBCommandExecutor().onCommand(alice, command, "cb", new String[]{"match"}); }
        finally { scanRadius = radius; }
        assertNotSame(previous, match); assertEquals(MatchState.END, previous.getMatchState());
        for (Runnable callback : delayed) callback.run();
        assertEquals(MatchState.CREATED, match.getMatchState()); assertFalse(balls.containsKey(BALL_MATCH_ID));
    }

    @Test void corruptedPersistenceStillAllowsFreeMatchGoals() throws Exception {
        Path invalid = directory.resolve("invalid.yml"); java.nio.file.Files.writeString(invalid, "broken: [");
        clubStore = new ClubStore(invalid, 0, s -> {});
        assertFalse(clubStore.available());
        Match free = ready(false); free.setMatchState(MatchState.IN_PROGRESS);
        assertTrue(free.touch(BALL_MATCH_ID, alice)); scoreBlue(free);
        assertEquals(1, free.getBlueScore());
        assertEquals("broken: [", java.nio.file.Files.readString(invalid));
    }
}
