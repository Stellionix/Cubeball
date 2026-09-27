# Configuration

CubeBall stores its settings in `plugins/CubeBall/config.yml`. After editing the file, apply changes with `/cb reload`.

## Ball

`ball.material`
: Falling block material used for the ball.

`ball.drop-item`
: Allows the ball to drop as an item if the entity breaks.

`ball.invulnerable`
: Protects the ball entity on server versions that support invulnerability.

`ball.spawn-particle.*`
: Controls particle type, amount, offsets, and speed when a ball is spawned.

## Match

`match.duration-seconds`
: Total match duration in seconds.

`match.max-goals`
: Ends the match once one team reaches this amount. `0` means unlimited.

`match.scan-radius`
: Radius used by `/cb match` to find arena marker blocks.

`match.countdown-step-ticks`
: Delay between `3`, `2`, `1`, and `GO`.

`match.round-restart-delay-ticks`
: Delay before restarting after a goal.

`match.title.*`
: Controls title fade timings and the sound played for match messages.

`match.goal-animation.firework.enabled`
: Spawns fireworks on goal celebration.

`match.goal-animation.effect.*`
: Controls the world effect played on goal blocks.

The default effect is `BONE_MEAL_USE`.
## Arena Materials

`arena.materials.ball-spawn`
: Marker block used to place the match ball spawn.

`arena.materials.blue-team-spawn`
: Marker block used for blue player spawn points.

`arena.materials.red-team-spawn`
: Marker block used for red player spawn points.

`arena.materials.blue-team-goal`
: Marker block used for the blue goal line.

`arena.materials.red-team-goal`
: Marker block used for the red goal line.

## Physics

`physics.player-search-radius`
: Distance used to find nearby players around the ball.

`physics.player-direct-hit-distance`
: Collision distance for a direct kick.

`physics.player-column-hit-distance`
: Extra collision distance when player and ball share the same X/Z block column.

`physics.kick.y-velocity.*`
: Vertical boost applied when standing, sneaking, or sprinting.

`physics.block-bounce.*`
: Bounce detection thresholds used by the repeating update task.

`physics.listener-bounce.*`
: Settings used when the server attempts to place the falling block.

`physics.ball-hit-sound.*`
: Sound name, volume, pitch, and minimum impact threshold for ball impacts.

## Tasks

`tasks.match-timer-period-ticks`
: Period of the match countdown scheduler.

`tasks.ball-update-period-ticks`
: Period of the main ball physics scheduler.

## Club capacity

```yaml
clubs:
  max-members: 0
```

`0` means unlimited members. A positive value limits registrations and accepted transfers, not the minimum required to start a match. Each side needs one connected member. Managers count only when explicitly registered as members. Lowering the limit does not remove existing members. Apply changes with `/cb reload`.

Persistent player and club data is stored separately in `clubs.yml`; reloading configuration does not reload this data file. See [Club matches](club-matches.md) for backup, recovery and verification instructions.
