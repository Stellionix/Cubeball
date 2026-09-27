package me.crylonz;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CubeBallConfigTest {

    @Test
    void resolvesLegacyNameWhenOnlyModernEnumConstantExists() {
        assertEquals("modern", CubeBall.resolveEffectName(" villager_plant_grow ", name -> {
            if (name.equals("BONE_MEAL_USE")) return "modern";
            throw new IllegalArgumentException(name);
        }));
    }

    @Test
    void resolvesModernNameOnLegacyServer() {
        assertEquals("legacy", CubeBall.resolveEffectName("BONE_MEAL_USE", name -> {
            if (name.equals("VILLAGER_PLANT_GROW")) return "legacy";
            throw new IllegalArgumentException(name);
        }));
    }

    @Test
    void missingEffectDoesNotThrow() {
        assertNull(CubeBall.resolveEffectName("BONE_MEAL_USE", name -> { throw new IllegalArgumentException(name); }));
        YamlConfiguration config = new YamlConfiguration();
        config.set("match.goal-animation.effect.name", "NOT_AN_EFFECT");
        CubeBall.applyConfigValues(config);
        assertEquals(CubeBall.resolveEffectName("BONE_MEAL_USE", Effect::valueOf), CubeBall.goalEffect);
    }

    @Test
    void appliesDefaultLikeConfigurationValues() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("ball.material", "SPONGE");
        config.set("ball.drop-item", false);
        config.set("ball.invulnerable", true);
        config.set("ball.spawn-particle.type", "CLOUD");
        config.set("match.duration-seconds", 300);
        config.set("match.max-goals", 0);
        config.set("match.goal-animation.effect.name", "VILLAGER_PLANT_GROW");
        config.set("arena.materials.ball-spawn", "EMERALD_BLOCK");
        config.set("physics.ball-hit-sound.name", "BLOCK_WOOL_HIT");
        config.set("tasks.match-timer-period-ticks", 20);
        config.set("tasks.ball-update-period-ticks", 2);

        CubeBall.applyConfigValues(config);

        assertEquals(Material.SPONGE, CubeBall.cubeBallBlock);
        assertEquals(false, CubeBall.ballDropItem);
        assertEquals(true, CubeBall.ballInvulnerable);
        assertEquals(Particle.CLOUD, CubeBall.ballSpawnParticle);
        assertEquals(300, CubeBall.matchDuration);
        assertEquals(0, CubeBall.maxGoal);
        assertEquals(Material.EMERALD_BLOCK, CubeBall.ballSpawnBlock);
        assertEquals(Sound.BLOCK_WOOL_HIT, CubeBall.ballHitSound);
        assertEquals(Effect.valueOf("BONE_MEAL_USE"), CubeBall.goalEffect);
        assertEquals(20, CubeBall.matchTimerTaskPeriodTicks);
        assertEquals(2, CubeBall.ballUpdateTaskPeriodTicks);
    }

    @Test
    void appliesCustomizedConfigurationValues() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("ball.material", "IRON_BLOCK");
        config.set("ball.drop-item", true);
        config.set("ball.invulnerable", false);
        config.set("ball.spawn-particle.type", "FLAME");
        config.set("match.duration-seconds", 42);
        config.set("match.max-goals", 5);
        config.set("match.scan-radius", 12);
        config.set("match.countdown-step-ticks", 10);
        config.set("match.round-restart-delay-ticks", 30);
        config.set("match.title.sound.name", "BLOCK_NOTE_BLOCK_BELL");
        config.set("match.goal-animation.firework.enabled", false);
        config.set("match.goal-animation.effect.name", "STEP_SOUND");
        config.set("match.goal-animation.effect.data", 1);
        config.set("arena.materials.ball-spawn", "GOLD_BLOCK");
        config.set("physics.ball-hit-sound.name", "ENTITY_EXPERIENCE_ORB_PICKUP");
        config.set("tasks.match-timer-period-ticks", 40);
        config.set("tasks.ball-update-period-ticks", 5);

        CubeBall.applyConfigValues(config);

        assertEquals(Material.IRON_BLOCK, CubeBall.cubeBallBlock);
        assertEquals(true, CubeBall.ballDropItem);
        assertEquals(false, CubeBall.ballInvulnerable);
        assertEquals(Particle.FLAME, CubeBall.ballSpawnParticle);
        assertEquals(42, CubeBall.matchDuration);
        assertEquals(5, CubeBall.maxGoal);
        assertEquals(12, CubeBall.scanRadius);
        assertEquals(Sound.BLOCK_NOTE_BLOCK_BELL, CubeBall.titleSound);
        assertEquals(false, CubeBall.spawnGoalFireworks);
        assertEquals(Effect.STEP_SOUND, CubeBall.goalEffect);
        assertEquals(1, CubeBall.goalEffectData);
        assertEquals(Material.GOLD_BLOCK, CubeBall.ballSpawnBlock);
        assertEquals(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, CubeBall.ballHitSound);
        assertEquals(40, CubeBall.matchTimerTaskPeriodTicks);
        assertEquals(5, CubeBall.ballUpdateTaskPeriodTicks);
    }
}
