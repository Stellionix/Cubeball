package me.crylonz;

import org.bukkit.*;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.*;

import static me.crylonz.CubeBall.*;
import static me.crylonz.MatchState.*;
import static org.bukkit.Bukkit.getServer;

enum Team {RED, BLUE, SPECTATOR}

enum MatchState {CREATED, READY, IN_PROGRESS, GOAL, PAUSED, OVERTIME, END}

public class Match {

    private final Random rand = new Random();
    private final ArrayList<Location> blueTeamGoalBlocks;
    private final ArrayList<Location> redTeamGoalBlocks;
    private final ArrayList<Player> blueTeam;
    private final ArrayList<Player> redTeam;
    private final ArrayList<Player> spectatorTeam;
    private MatchState matchState;
    private Location ballSpawn;
    private String lastTouchPlayer;
    private ArrayList<Location> blueTeamSpawns;
    private ArrayList<Location> redTeamSpawns;
    private int blueScore;
    private int redScore;
    private final GoalContacts contacts = new GoalContacts();
    private final List<Integer> tasks = new ArrayList<>();
    private int taskGeneration;
    private String blueClub, redClub;
    private String blueName = "BLUE", redName = "RED";
    private final Map<UUID, Team> roster = new HashMap<>();
    private boolean frozen;

    public Match(String blue, String red) {
        this();
        ClubStore.Club b = clubStore.club(blue), r = clubStore.club(red);
        if (b.id().equals(r.id())) throw new IllegalArgumentException("Choose two different clubs.");
        blueClub = b.id(); redClub = r.id(); blueName = b.name(); redName = r.name();
        refreshRoster();
    }

    public boolean isClubMatch() { return blueClub != null; }

    private void refreshRoster() {
        if (!isClubMatch() || frozen) return;
        clubStore.club(blueClub); clubStore.club(redClub);
        roster.clear(); blueTeam.clear(); redTeam.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String club = clubStore.membership(player.getUniqueId());
            if (blueClub.equals(club)) { roster.put(player.getUniqueId(), Team.BLUE); blueTeam.add(player); }
            if (redClub.equals(club)) { roster.put(player.getUniqueId(), Team.RED); redTeam.add(player); }
        }
    }

    public void reconnect(Player player) {
        if (!isClubMatch() || !frozen || matchState == END) return;
        Team team = roster.get(player.getUniqueId());
        blueTeam.removeIf(p -> p.getUniqueId().equals(player.getUniqueId()));
        redTeam.removeIf(p -> p.getUniqueId().equals(player.getUniqueId()));
        if (team == Team.BLUE) blueTeam.add(player);
        if (team == Team.RED) redTeam.add(player);
    }

    public Team teamOf(Player player) {
        if (isClubMatch() && frozen) return roster.getOrDefault(player.getUniqueId(), Team.SPECTATOR);
        if (blueTeam.contains(player)) return Team.BLUE;
        if (redTeam.contains(player)) return Team.RED;
        return Team.SPECTATOR;
    }

    public boolean touch(String ballId, Player player) {
        if (!BALL_MATCH_ID.equals(ballId) || (matchState != IN_PROGRESS && matchState != OVERTIME)
                || teamOf(player) == Team.SPECTATOR) return false;
        contacts.touch(player.getUniqueId(), teamOf(player));
        lastTouchPlayer = player.getName();
        if (clubStore != null && clubStore.available()) {
            try { clubStore.remember(player.getUniqueId(), player.getName()); }
            catch (IllegalStateException e) { log.warning(e.getMessage()); }
        }
        return true;
    }

    private void later(Runnable action, long delay) {
        int generation = taskGeneration;
        tasks.add(getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            if (generation == taskGeneration && matchState != END) action.run();
        }, delay));
    }

    private void cancelTasks() {
        taskGeneration++;
        for (int task : tasks) getServer().getScheduler().cancelTask(task);
        tasks.clear();
    }

    public void stop() {
        matchState = END; cancelTasks(); contacts.reset(); lastTouchPlayer = null; removeBall();
    }

    public Match() {
        blueTeam = new ArrayList<>();
        redTeam = new ArrayList<>();
        spectatorTeam = new ArrayList<>();
        blueTeamGoalBlocks = new ArrayList<>();
        redTeamGoalBlocks = new ArrayList<>();
        blueScore = 0;
        redScore = 0;
        matchState = CREATED;
    }

    public void scanSpawn(Player p) {

        ballSpawn = null;
        blueTeamSpawns = new ArrayList<>();
        redTeamSpawns = new ArrayList<>();

        int radius = scanRadius;
        final org.bukkit.block.Block block = p.getLocation().getBlock();
        for (int x = -(radius); x <= radius; x++) {
            for (int y = -(radius); y <= radius; y++) {
                for (int z = -(radius); z <= radius; z++) {
                    if (block.getRelative(x, y, z).getType() == ballSpawnBlock) {
                        ballSpawn = block.getRelative(x, y + 3, z).getLocation().add(.5, 0, .5);
                    }
                    if (block.getRelative(x, y, z).getType() == blueTeamSpawnBlock) {
                        blueTeamSpawns.add(block.getRelative(x, y + 2, z).getLocation());
                    }
                    if (block.getRelative(x, y, z).getType() == redTeamSpawnBlock) {
                        redTeamSpawns.add(block.getRelative(x, y + 2, z).getLocation());
                    }
                    if (block.getRelative(x, y, z).getType() == blueTeamGoalBlock) {
                        blueTeamGoalBlocks.add(block.getRelative(x, y, z).getLocation());
                    }
                    if (block.getRelative(x, y, z).getType() == redTeamGoalBlock) {
                        redTeamGoalBlocks.add(block.getRelative(x, y, z).getLocation());
                    }
                }
            }
        }

        if (ballSpawn != null && blueTeamSpawns.size() > 0 && redTeamSpawns.size() > 0 &&
                blueTeamGoalBlocks.size() > 0 && redTeamGoalBlocks.size() > 0) {
            p.sendMessage("--- MATCH READY ---");
            matchState = READY;
        } else {
            p.sendMessage("--- ERROR ---");
        }
        p.sendMessage("Ball Spawn : " + (ballSpawn != null ? ChatColor.GREEN + "OK" : ChatColor.RED + "KO"));
        p.sendMessage("Blue Spawn : " + (blueTeamSpawns.size() != 0 ? ChatColor.GREEN + "OK" : ChatColor.RED + "KO") + " (" + blueTeamSpawns.size() + ")");
        p.sendMessage("Red Spawn  : " + (redTeamSpawns.size() != 0 ? ChatColor.GREEN + "OK" : ChatColor.RED + "KO") + " (" + redTeamSpawns.size() + ")");
        p.sendMessage("Blue Goal  : " + (blueTeamGoalBlocks.size() != 0 ? ChatColor.GREEN + "OK" : ChatColor.RED + "KO") + " (" + blueTeamGoalBlocks.size() + ")");
        p.sendMessage("Red Goal   : " + (redTeamGoalBlocks.size() != 0 ? ChatColor.GREEN + "OK" : ChatColor.RED + "KO") + " (" + redTeamGoalBlocks.size() + ")");
        p.sendMessage("------------------");
        p.sendMessage(isClubMatch() ? blueName + " vs " + redName + " — use /cb start" : "Next step : Use /cb team to generate team");
    }

    public void start(Player p) {
        if (matchState.equals(READY)) {
            try { refreshRoster(); }
            catch (IllegalArgumentException | IllegalStateException e) { p.sendMessage(e.getMessage()); return; }
            if (isClubMatch() && (blueTeam.isEmpty() || redTeam.isEmpty())) {
                p.sendMessage("[CubeBall] At least one connected member of each club is required."); return;
            }
            if (!blueTeam.isEmpty() || !redTeam.isEmpty()) {
                frozen = isClubMatch();
                startDelayedRound();
                matchTimer = matchDuration;
                matchState = IN_PROGRESS;

                p.sendMessage("[CubeBall] " + ChatColor.GREEN + "Match starting !");
                getAllPlayer().forEach(player -> {
                    player.sendMessage("[CubeBall] " + ChatColor.GREEN + "Match started ! Duration : " + ChatColor.GOLD + (matchTimer / 60) + ":" + (matchTimer - ((matchTimer / 60) * 60)));
                    player.sendMessage("[CubeBall] " + ChatColor.GREEN + "Max goals : " + (maxGoal == 0 ? "UNLIMITED" : maxGoal));
                });
            } else {
                p.sendMessage("[CubeBall] " + ChatColor.RED + "You need to add players to team (/cb team blue|red) <Player>");
            }
        } else {
            p.sendMessage("[CubeBall] " + ChatColor.RED + "Match is not ready or already started");
        }
    }

    private void startDelayedRound() {
        contacts.reset(); lastTouchPlayer = null;
        blueTeam.forEach(player -> {
            int randomIndex = rand.nextInt(blueTeamSpawns.size());
            player.teleport(blueTeamSpawns.get(randomIndex));
        });

        redTeam.forEach(player -> {
            int randomIndex = rand.nextInt(redTeamSpawns.size());
            player.teleport(redTeamSpawns.get(randomIndex));
        });

        later(() -> sendMessageToAllPlayer("3", "", 1), countdownStepTicks);
        later(() -> sendMessageToAllPlayer("2", "", 1), countdownStepTicks * 2L);
        later(() -> sendMessageToAllPlayer("1", "", 1), countdownStepTicks * 3L);
        later(() -> {
            sendMessageToAllPlayer("GO !", "", 1);
            startRound();
        }, countdownStepTicks * 4L);
    }

    private void startRound() {
        matchState = matchTimer > 0 ? IN_PROGRESS : OVERTIME;
        removeBall();
        CubeBall.generateBall(BALL_MATCH_ID, ballSpawn, null);
    }

    public boolean addPlayerToTeam(Player p, Team team) {
        if (isClubMatch()) return false;
        if (p != null) {
            if (team.equals(Team.BLUE)) {
                if (!blueTeam.contains(p)) {
                    blueTeam.add(p);
                }
                redTeam.remove(p);
                spectatorTeam.remove(p);
            } else if (team.equals(Team.RED)) {
                if (!redTeam.contains(p)) {
                    redTeam.add(p);
                }
                blueTeam.remove(p);
                spectatorTeam.remove(p);
            } else {
                if (!spectatorTeam.contains(p)) {
                    spectatorTeam.add(p);
                }
                blueTeam.remove(p);
                redTeam.remove(p);
            }
            return true;
        }
        return false;
    }

    public void checkGoal(Location ballLocation) {
        if (matchState.equals(IN_PROGRESS) || matchState.equals(OVERTIME)) {

            for (Location blockLocation : blueTeamGoalBlocks) {
                if (ballLocation.getBlockX() == blockLocation.getBlockX() &&
                        Objects.equals(ballLocation.getWorld(), blockLocation.getWorld()) &&
                        ballLocation.getBlockZ() == blockLocation.getBlockZ()) {
                    goal(Team.RED);
                    return;
                }
            }

            for (Location blockLocation : redTeamGoalBlocks) {
                if (ballLocation.getBlockX() == blockLocation.getBlockX() &&
                        Objects.equals(ballLocation.getWorld(), blockLocation.getWorld()) &&
                        ballLocation.getBlockZ() == blockLocation.getBlockZ()) {
                    goal(Team.BLUE);
                    return;
                }
            }
        }
    }

    private void goal(Team team) {
        GoalContacts.Credit credit = contacts.credit(team);
        lastTouchPlayer = credit.scorer() == null ? null : lastTouchPlayer;
        if (clubStore != null && clubStore.available()) {
            try { clubStore.recordGoal(credit.scorer(), credit.assistant()); }
            catch (IllegalStateException | IllegalArgumentException e) { log.warning(e.getMessage()); }
        }
        contacts.reset();
        if (Team.BLUE.equals(team)) {
            blueScore++;
            triggerGoalAnimation(Team.BLUE);

        } else {
            redScore++;
            triggerGoalAnimation(Team.RED);
        }

        if (matchState.equals(IN_PROGRESS) && (maxGoal == 0 || (blueScore != maxGoal && redScore != maxGoal))) {
            sendScoreToPlayer();
            matchState = GOAL;
            later(this::startDelayedRound, roundRestartDelayTicks);
        } else {
            matchState = GOAL;
            endMatch();
        }
        destroyBall(BALL_MATCH_ID);
    }

    public void endMatch() {
        String title;
        if (getBlueScore() > getRedScore()) {
            title = ChatColor.BLUE + blueName + ChatColor.GOLD + " TEAM WIN !";
        } else if (getBlueScore() < getRedScore()) {
            title = ChatColor.RED + redName + ChatColor.GOLD + " TEAM WIN !";
        } else {
            title = ChatColor.GOLD + "OVERTIME !";
            setMatchState(OVERTIME);
        }

        String score = ChatColor.BLUE.toString() + getBlueScore() + ChatColor.WHITE + " - " + ChatColor.RED + getRedScore();
        sendMessageToAllPlayer(title, score, 3);
        if (!getMatchState().equals(OVERTIME)) {
            stop();
        }
    }

    public void sendScoreToPlayer() {
        String title = ChatColor.BLUE.toString() + blueScore + ChatColor.WHITE + " - " + ChatColor.RED + redScore;
        String subtitle = ChatColor.BOLD.toString() + ChatColor.GOLD + (lastTouchPlayer == null ? "GOAL ! " : lastTouchPlayer.toUpperCase(Locale.ROOT) + " SCORES ! ") + ChatColor.RESET
                + "(" + computeSpeedGoal() + " km/h)";
        sendMessageToAllPlayer(title, subtitle, 3);
    }

    public double computeSpeedGoal() {

        Ball ball = balls.get(BALL_MATCH_ID);

        if (ball != null && ball.getBall() != null) {
            return Math.round((Math.abs((ball.getLastVelocity().getX())) + Math.abs((ball.getLastVelocity().getY())) + Math.abs((ball.getLastVelocity().getZ()))) * 100);
        }
        return 0;
    }

    public void sendMessageToAllPlayer(String title, String subtitle, int duration) {
        blueTeam.forEach(player -> {
            if (player != null) {
                player.sendTitle(title, subtitle, titleFadeInTicks, duration * titleStayPerSecondTicks, titleFadeOutTicks);
                player.playSound(player.getLocation(), titleSound, titleSoundVolume, titleSoundPitch);
            }
        });

        redTeam.forEach(player -> {
            if (player != null) {
                player.sendTitle(title, subtitle, titleFadeInTicks, duration * titleStayPerSecondTicks, titleFadeOutTicks);
                player.playSound(player.getLocation(), titleSound, titleSoundVolume, titleSoundPitch);
            }
        });

        spectatorTeam.forEach(player -> {
            if (player != null) {
                player.sendTitle(title, subtitle, titleFadeInTicks, duration * titleStayPerSecondTicks, titleFadeOutTicks);
                player.playSound(player.getLocation(), titleSound, titleSoundVolume, titleSoundPitch);
            }
        });
    }

    public void triggerGoalAnimation(Team team) {
        if (team.equals(Team.BLUE)) {
            redTeamGoalBlocks.forEach(block -> {
                playGoalAnimation(block);
            });
        } else {
            blueTeamGoalBlocks.forEach(block -> {
                playGoalAnimation(block);
            });

        }
    }

    private void spawnFirework(Location block) {
        Objects.requireNonNull(block.getWorld()).spawn(block.getBlock().getLocation(), Firework.class);
    }

    private void playGoalAnimation(Location block) {
        if (spawnGoalFireworks) {
            spawnFirework(block);
        }
        if (goalEffect != null) {
            Objects.requireNonNull(block.getWorld()).playEffect(block.getBlock().getLocation(), goalEffect, goalEffectData);
        }
    }

    public void displayTeams(Player p) {
        p.sendMessage(blueName + " : " + this.blueTeam.size() + " player(s)");
        this.blueTeam.forEach(player -> {
            if (player != null) {
                p.sendMessage("-" + ChatColor.BLUE + player.getDisplayName());
            }
        });

        p.sendMessage(redName + " : " + this.redTeam.size() + " player(s)");
        this.redTeam.forEach(player -> {
            if (player != null) {
                p.sendMessage("-" + ChatColor.RED + player.getDisplayName());
            }
        });

        p.sendMessage("SPECTATOR TEAM : " + this.spectatorTeam.size() + " player(s)");
        this.spectatorTeam.forEach(player -> {
            if (player != null) {
                p.sendMessage("-" + ChatColor.GREEN + player.getDisplayName());
            }
        });
    }

    public void setLastTouchPlayer(String lastTouchPlayer) {
        this.lastTouchPlayer = lastTouchPlayer;
    }

    public ArrayList<Player> getBlueTeam() {
        return blueTeam;
    }

    public ArrayList<Player> getRedTeam() {
        return redTeam;
    }

    public ArrayList<Player> getSpectatorTeam() {
        return spectatorTeam;
    }

    public ArrayList<Player> getAllPlayer() {
        ArrayList<Player> team = new ArrayList<>();
        team.addAll(getRedTeam());
        team.addAll(getBlueTeam());
        team.addAll(getSpectatorTeam());
        return team;
    }

    public int getBlueScore() {
        return blueScore;
    }

    public int getRedScore() {
        return redScore;
    }

    public MatchState getMatchState() {
        return matchState;
    }

    public void setMatchState(MatchState matchState) {
        this.matchState = matchState;
    }

    public void removeBall() {
        destroyBall(BALL_MATCH_ID);
    }

    public boolean pause() {
        if (matchState.equals(IN_PROGRESS) || matchState.equals(OVERTIME)) {
            matchState = PAUSED;
            cancelTasks(); contacts.reset(); lastTouchPlayer = null;
            removeBall();
            return true;
        }
        return false;
    }

    public boolean resume() {
        if (matchState.equals(PAUSED)) {
            matchState = matchTimer > 0 ? IN_PROGRESS : OVERTIME;
            startDelayedRound();
            return true;
        }
        return false;
    }
}
