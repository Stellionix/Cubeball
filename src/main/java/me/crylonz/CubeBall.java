package me.crylonz;

import org.bstats.bukkit.Metrics;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.logging.Logger;

import static java.lang.Math.abs;
import static me.crylonz.MatchState.IN_PROGRESS;
import static me.crylonz.MatchState.OVERTIME;

public class CubeBall extends JavaPlugin {

    interface ConfigGateway {
        void reloadConfig();

        FileConfiguration getConfig();
    }

    public static File configFile;
    public static FileConfiguration customConfig = null;
    public static HashMap<String, Ball> balls = new HashMap<>();

    public static Match match;
    public static ClubStore clubStore;
    public static int maxClubMembers;

    public static String BALL_MATCH_ID = "BALL_MATCH_ID_DONT_USE_IT";
    public static Plugin plugin;
    static ConfigGateway configGateway;
    public static int matchTimer = 0;
    public final static Logger log = Logger.getLogger("Minecraft");

    // config
    public static Material cubeBallBlock = Material.IRON_BLOCK;
    public static int matchDuration = 300;
    public static int maxGoal = 0;
    public static Material ballSpawnBlock = Material.EMERALD_BLOCK;
    public static Material blueTeamSpawnBlock = Material.BLUE_WOOL;
    public static Material blueTeamGoalBlock = Material.BLUE_CONCRETE;
    public static Material redTeamSpawnBlock = Material.RED_WOOL;
    public static Material redTeamGoalBlock = Material.RED_CONCRETE;
    public static boolean ballDropItem = false;
    public static boolean ballInvulnerable = true;
    public static Particle ballSpawnParticle = Particle.CLOUD;
    public static int ballSpawnParticleCount = 12;
    public static double ballSpawnParticleOffsetX = 0.2;
    public static double ballSpawnParticleOffsetY = 0.2;
    public static double ballSpawnParticleOffsetZ = 0.2;
    public static double ballSpawnParticleSpeed = 0.01;
    public static Sound ballHitSound = Sound.BLOCK_WOOL_HIT;
    public static float ballHitSoundVolume = 10.0F;
    public static float ballHitSoundPitch = 1.0F;
    public static double ballHitSoundThreshold = 0.1;
    public static int ballPlayerSearchRadius = 10;
    public static double ballDirectHitDistance = 1.0;
    public static double ballColumnHitDistance = 2.0;
    public static double baseKickYVelocity = 0.15;
    public static double sneakingKickYVelocity = 0.5;
    public static double sprintingKickYVelocity = 0.25;
    public static int blockBounceDelayTicks = 3;
    public static double blockBounceThreshold = 0.2;
    public static int ballUpdateTaskPeriodTicks = 2;
    public static int matchTimerTaskPeriodTicks = 20;
    public static int scanRadius = 75;
    public static int countdownStepTicks = 20;
    public static int roundRestartDelayTicks = 60;
    public static int titleFadeInTicks = 1;
    public static int titleStayPerSecondTicks = 20;
    public static int titleFadeOutTicks = 1;
    public static Sound titleSound = Sound.BLOCK_BEACON_ACTIVATE;
    public static float titleSoundVolume = 1.0F;
    public static float titleSoundPitch = 3.0F;
    public static Effect goalEffect = resolveEffectName("BONE_MEAL_USE", Effect::valueOf);
    public static int goalEffectData = 3;
    public static boolean spawnGoalFireworks = true;
    public static double listenerBounceDivisor = 1.5;
    public static double listenerBounceMaxYVelocity = 0.5;
    public static double listenerStopVelocityThreshold = 0.001;
    public static double listenerStopMinYVelocity = 0.025;
    public static double listenerFetchDistanceSquared = 2.0;

    public static void generateBall(String id, Location location, Vector lastVelocity) {

        if (balls.get(id) != null) {
            throw new IllegalStateException("Same ID cannot be put on the same ball");
        }

        BlockData blockData = Bukkit.createBlockData(cubeBallBlock);
        FallingBlock block = Objects.requireNonNull(location.getWorld()).spawnFallingBlock(location, blockData);
        block.setMetadata("ballID", new FixedMetadataValue(plugin, id));
        applyBallVisualSettings(block);
        block.getWorld().spawnParticle(
                ballSpawnParticle,
                block.getLocation(),
                ballSpawnParticleCount,
                ballSpawnParticleOffsetX,
                ballSpawnParticleOffsetY,
                ballSpawnParticleOffsetZ,
                ballSpawnParticleSpeed
        );

        Ball ball = new Ball();
        ball.setId(id);
        ball.setBall(block);

        if (lastVelocity != null) {
            ball.getLastVelocity().setX(0);
            ball.getLastVelocity().setZ(0);
        }

        ball.setPlayerCollisionTick(0);
        balls.put(id, ball);

        // Some server/client combinations do not keep the glowing flag on the first spawn packet.
        Bukkit.getScheduler().runTask(plugin, () -> {
            Ball spawnedBall = balls.get(id);
            if (spawnedBall != null && spawnedBall.getBall() != null) {
                applyBallVisualSettings(spawnedBall.getBall());
            }
        });
    }

    public static void destroyBall(String id) {
        Ball ball = balls.get(id);
        if (ball != null) {
            if (ball.getBall() != null) {
                ball.getBall().remove();
            }
            balls.remove(id);
        }
    }

    public void onEnable() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new CubeBallListener(), this);

        plugin = this;
        configGateway = new ConfigGateway() {
            @Override
            public void reloadConfig() {
                CubeBall.this.reloadConfig();
            }

            @Override
            public FileConfiguration getConfig() {
                return CubeBall.this.getConfig();
            }
        };

        Metrics metrics = new Metrics(this, 17634);

        launchRepeatingTask();

        Objects.requireNonNull(this.getCommand("cb"), "Command cb not found")
                .setExecutor(new CBCommandExecutor());

        Objects.requireNonNull(getCommand("cb")).setTabCompleter(new CBTabCompletion());

        configFile = new File(getDataFolder(), "config.yml");
        saveDefaultConfig();
        reloadConfig();
        applyConfigValues(getConfig());
        clubStore = new ClubStore(getDataFolder().toPath().resolve("clubs.yml"), maxClubMembers, getLogger()::severe);
        for (Player player : Bukkit.getOnlinePlayers()) rememberPlayer(player);
    }

    public void onDisable() {
        if (match != null) { match.stop(); match = null; }
        balls.forEach((key, value) -> {
            if (value.getBall() != null) {
                value.getBall().remove();
            }
        });
        balls.clear();
    }

    private void launchRepeatingTask() {

        getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (match != null && match.getMatchState().equals(IN_PROGRESS)) {
                matchTimer--;

                if (matchTimer % 60 == 0 && matchTimer > 0) {
                    match.getAllPlayer().forEach(player -> {
                        if (player != null) {
                            player.sendMessage("[CubeBall] " + ChatColor.GOLD + matchTimer / 60 + " min left !");
                        }

                    });
                }
                if (matchTimer == 30 || matchTimer == 15 || matchTimer <= 10 && matchTimer > 0) {
                    match.getAllPlayer().forEach(player -> {
                        if (player != null) {
                            player.sendMessage("[CubeBall] " + ChatColor.GOLD + matchTimer + " sec left !");
                        }
                    });
                }
                if (matchTimer <= 0) {
                    match.endMatch();
                    if (match.getMatchState() != OVERTIME) {
                        match = null;
                    }
                }
            }
        }, 0, matchTimerTaskPeriodTicks);


        getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {

            for (Iterator<Map.Entry<String, Ball>> iterator = new ArrayList<>(balls.entrySet()).iterator(); iterator.hasNext(); ) {
                Map.Entry<String, Ball> entry = iterator.next();
                String id = entry.getKey();
                Ball ballData = entry.getValue();
                if (ballData.getBall() != null) {
                    applyBallVisualSettings(ballData.getBall());
                    ballData.getBall().setTicksLived(1);

                    ballData.getBall().getNearbyEntities(ballPlayerSearchRadius, ballPlayerSearchRadius, ballPlayerSearchRadius)
                            .stream().filter(entity -> entity instanceof Player)
                            .forEach(p -> {
                                Player player = (Player) p;
                                if (BALL_MATCH_ID.equals(id) && (match == null || match.teamOf(player) == Team.SPECTATOR
                                        || (match.getMatchState() != IN_PROGRESS && match.getMatchState() != OVERTIME))) return;
                                // if player is colliding the ball
                                if (player.getLocation().distance(ballData.getBall().getLocation()) < ballDirectHitDistance || (
                                        player.getLocation().distance(ballData.getBall().getLocation()) < ballColumnHitDistance &&
                                                Math.floor(ballData.getBall().getLocation().getX()) == Math.floor(player.getLocation().getX()) &&
                                                Math.floor(ballData.getBall().getLocation().getZ()) == Math.floor(player.getLocation().getZ()))) {

                                    // compute velocity to the ball
                                    double yVeclocity = baseKickYVelocity;

                                    if (player.isSneaking()) {
                                        yVeclocity = sneakingKickYVelocity;
                                    } else if (player.isSprinting()) {
                                        yVeclocity = sprintingKickYVelocity;
                                    }

                                    Vector velocity = ballData.getBall().getVelocity();
                                    velocity.setY(ballData.getBall().getVelocity().getY() + yVeclocity + player.getVelocity().getY() / 2);
                                    velocity.setX(ballData.getBall().getVelocity().getX() + player.getLocation().getDirection().getX() / 2);
                                    velocity.setZ(ballData.getBall().getVelocity().getZ() + player.getLocation().getDirection().getZ() / 2);

                                    // if player is not moving, create bouncing on it
                                    if (abs(player.getVelocity().getX() + player.getVelocity().getY() + player.getVelocity().getZ()) == 0) {
                                        velocity.setY(0);
                                        velocity.setX(0);
                                        velocity.setZ(0);
                                    }

                                    // apply ball trajectory
                                    ballData.getBall().setVelocity(velocity);
                                    ballData.getBall().setGravity(true);
                                    playBallHitSound(ballData.getBall().getLocation());
                                    ballData.setPlayerCollisionTick(0);

                                    if (match != null) match.touch(id, player);
                                }
                            });

                    //compute bouncing on other blocks
                    if (ballData.getPlayerCollisionTick() > blockBounceDelayTicks) {

                        boolean zBouncing = abs(ballData.getLastVelocity().getZ()) - abs(ballData.getBall().getVelocity().getZ()) > blockBounceThreshold && ballData.getBall().getVelocity().getZ() == 0;
                        boolean xBouncing = abs(ballData.getLastVelocity().getX()) - abs(ballData.getBall().getVelocity().getX()) > blockBounceThreshold && ballData.getBall().getVelocity().getX() == 0;
                        boolean yBouncing = abs(ballData.getLastVelocity().getY()) - abs(ballData.getBall().getVelocity().getY()) > blockBounceThreshold && ballData.getBall().getVelocity().getY() == 0;

                        if (zBouncing) {
                            ballData.getBall().setVelocity(ballData.getBall().getVelocity().setZ(-ballData.getLastVelocity().getZ()));
                            ballData.getBall().getVelocity().setZ(-ballData.getLastVelocity().getZ());
                            playBallHitSound(ballData.getBall().getLocation());
                        }
                        if (xBouncing) {
                            ballData.getBall().setVelocity(ballData.getBall().getVelocity().setX(-ballData.getLastVelocity().getX()));
                            ballData.getBall().getVelocity().setX(-ballData.getLastVelocity().getX());
                            playBallHitSound(ballData.getBall().getLocation());
                        }
                        if (yBouncing) {
                            ballData.getBall().setGravity(true);
                            ballData.getBall().setVelocity(ballData.getBall().getVelocity().setY(-ballData.getLastVelocity().getY()));
                            playBallHitSound(ballData.getBall().getLocation());
                        }
                    }

                    if (match != null && ballData.getId().equals(BALL_MATCH_ID)) {
                        match.checkGoal(ballData.getBall().getLocation());
                    }

                    ballData.setLastVelocity(ballData.getBall().getVelocity().clone());
                    ballData.setPlayerCollisionTick(ballData.getPlayerCollisionTick() + 1);
                }
            }
        }, 0, ballUpdateTaskPeriodTicks);
    }

    public static void playBallHitSound(Location location) {
        location.getWorld().playSound(location, ballHitSound, ballHitSoundVolume, ballHitSoundPitch);
    }

    private static void applyBallVisualSettings(FallingBlock block) {
        block.setDropItem(ballDropItem);
        block.setInvulnerable(ballInvulnerable);
    }

    public static void reloadPluginConfiguration() {
        configGateway.reloadConfig();
        applyConfigValues(configGateway.getConfig());
    }

    static void applyConfigValues(FileConfiguration config) {
        maxClubMembers = Math.max(0, config.getInt("clubs.max-members", 0));
        if (clubStore != null) clubStore.setMaxMembers(maxClubMembers);
        cubeBallBlock = material(config, "ball.material", Material.IRON_BLOCK);
        ballDropItem = config.getBoolean("ball.drop-item", false);
        ballInvulnerable = config.getBoolean("ball.invulnerable", true);
        ballSpawnParticle = particle(config, "ball.spawn-particle.type", Particle.CLOUD);
        ballSpawnParticleCount = config.getInt("ball.spawn-particle.count", 12);
        ballSpawnParticleOffsetX = config.getDouble("ball.spawn-particle.offset.x", 0.2);
        ballSpawnParticleOffsetY = config.getDouble("ball.spawn-particle.offset.y", 0.2);
        ballSpawnParticleOffsetZ = config.getDouble("ball.spawn-particle.offset.z", 0.2);
        ballSpawnParticleSpeed = config.getDouble("ball.spawn-particle.speed", 0.01);

        matchDuration = config.getInt("match.duration-seconds", 300);
        maxGoal = config.getInt("match.max-goals", 0);
        scanRadius = config.getInt("match.scan-radius", 75);
        countdownStepTicks = config.getInt("match.countdown-step-ticks", 20);
        roundRestartDelayTicks = config.getInt("match.round-restart-delay-ticks", 60);
        titleFadeInTicks = config.getInt("match.title.fade-in-ticks", 1);
        titleStayPerSecondTicks = config.getInt("match.title.stay-per-second-ticks", 20);
        titleFadeOutTicks = config.getInt("match.title.fade-out-ticks", 1);
        titleSound = sound(config, "match.title.sound.name", Sound.BLOCK_BEACON_ACTIVATE);
        titleSoundVolume = (float) config.getDouble("match.title.sound.volume", 1.0);
        titleSoundPitch = (float) config.getDouble("match.title.sound.pitch", 3.0);
        goalEffect = effect(config, "match.goal-animation.effect.name");
        goalEffectData = config.getInt("match.goal-animation.effect.data", 3);
        spawnGoalFireworks = config.getBoolean("match.goal-animation.firework.enabled", true);

        ballSpawnBlock = material(config, "arena.materials.ball-spawn", Material.EMERALD_BLOCK);
        blueTeamSpawnBlock = material(config, "arena.materials.blue-team-spawn", Material.BLUE_WOOL);
        blueTeamGoalBlock = material(config, "arena.materials.blue-team-goal", Material.BLUE_CONCRETE);
        redTeamSpawnBlock = material(config, "arena.materials.red-team-spawn", Material.RED_WOOL);
        redTeamGoalBlock = material(config, "arena.materials.red-team-goal", Material.RED_CONCRETE);

        ballHitSound = sound(config, "physics.ball-hit-sound.name", Sound.BLOCK_WOOL_HIT);
        ballHitSoundVolume = (float) config.getDouble("physics.ball-hit-sound.volume", 10.0);
        ballHitSoundPitch = (float) config.getDouble("physics.ball-hit-sound.pitch", 1.0);
        ballHitSoundThreshold = config.getDouble("physics.ball-hit-sound.minimum-impact", 0.1);
        ballPlayerSearchRadius = config.getInt("physics.player-search-radius", 10);
        ballDirectHitDistance = config.getDouble("physics.player-direct-hit-distance", 1.0);
        ballColumnHitDistance = config.getDouble("physics.player-column-hit-distance", 2.0);
        baseKickYVelocity = config.getDouble("physics.kick.y-velocity.base", 0.15);
        sneakingKickYVelocity = config.getDouble("physics.kick.y-velocity.sneaking", 0.5);
        sprintingKickYVelocity = config.getDouble("physics.kick.y-velocity.sprinting", 0.25);
        blockBounceDelayTicks = config.getInt("physics.block-bounce.delay-ticks", 3);
        blockBounceThreshold = config.getDouble("physics.block-bounce.threshold", 0.2);
        listenerBounceDivisor = config.getDouble("physics.listener-bounce.divisor", 1.5);
        listenerBounceMaxYVelocity = config.getDouble("physics.listener-bounce.max-y-velocity", 0.5);
        listenerStopVelocityThreshold = config.getDouble("physics.listener-bounce.stop-velocity-threshold", 0.001);
        listenerStopMinYVelocity = config.getDouble("physics.listener-bounce.stop-min-y-velocity", 0.025);
        listenerFetchDistanceSquared = config.getDouble("physics.listener-bounce.fetch-distance-squared", 2.0);
        matchTimerTaskPeriodTicks = config.getInt("tasks.match-timer-period-ticks", 20);
        ballUpdateTaskPeriodTicks = config.getInt("tasks.ball-update-period-ticks", 2);
    }

    private static Material material(FileConfiguration config, String path, Material fallback) {
        return Material.valueOf(config.getString(path, fallback.name()).toUpperCase(Locale.ROOT));
    }

    static void rememberPlayer(Player player) {
        if (clubStore != null && clubStore.available()) {
            try { clubStore.remember(player.getUniqueId(), player.getName()); }
            catch (IllegalStateException e) { log.warning(e.getMessage()); }
        }
    }

    private static Sound sound(FileConfiguration config, String path, Sound fallback) {
        return Sound.valueOf(config.getString(path, fallback.name()).toUpperCase(Locale.ROOT));
    }

    private static Particle particle(FileConfiguration config, String path, Particle fallback) {
        return Particle.valueOf(config.getString(path, fallback.name()).toUpperCase(Locale.ROOT));
    }

    private static Effect effect(FileConfiguration config, String path) {
        String name = config.getString(path, "BONE_MEAL_USE");
        Effect resolved = resolveEffectName(name, Effect::valueOf);
        if (resolved != null) return resolved;
        log.warning("[CubeBall] Unknown effect '" + name + "' at " + path
                + "; using the default goal effect if supported.");
        return resolveEffectName("BONE_MEAL_USE", Effect::valueOf);
    }

    // Resolve by name: Paper can retain a deprecated static alias without retaining an enum constant.
    static <T> T resolveEffectName(String name, java.util.function.Function<String, T> lookup) {
        String normalized = name.trim().toUpperCase(Locale.ROOT);
        String[] candidates = switch (normalized) {
            case "VILLAGER_PLANT_GROW", "BONE_MEAL_USE" -> new String[]{"BONE_MEAL_USE", "VILLAGER_PLANT_GROW"};
            default -> new String[]{normalized};
        };
        for (String candidate : candidates) {
            try { return lookup.apply(candidate); }
            catch (IllegalArgumentException ignored) { /* Try the equivalent name on older servers. */ }
        }
        return null;
    }
}


