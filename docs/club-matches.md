# Club matches and server verification

## Complete example

Alice, Bob and Carol must have joined the server at least once. An administrator can run the following from console or in game:

```text
cb club create north North United
cb club create south South City
cb club manager north Alice
cb club manager south Bob
cb club add north Alice
cb club add north Carol
cb club add south Bob
cb club list
cb club info north
```

Use a leading `/` in game. Build an arena as described in [Arena setup](arena-setup.md). With all three members connected, an administrator stands near its markers and runs:

```text
/cb match north south
/cb start
```

North United occupies blue, South City red. Participants are refreshed at start; at least one connected member of each club is required, and unequal squads are allowed. The connected squads are then frozen until the match ends. Reconnecting members recover their side. Players who were absent at start and new club members do not join automatically.

While the match runs, Alice can propose Carol's transfer:

```text
/cb club transfer Carol south
```

Bob runs `/cb club transfers`, copies the full transfer UUID, then runs `/cb club accept <transfer>`. Carol's current club changes immediately, but her side in the ongoing match remains blue. She plays for South City in the next match. Alice can cancel a pending proposal, or Bob can reject it.

## Goal attribution and storage

Both free and club matches count goals and assists, using only the match ball. The last touching player receives the goal if their side scores. An own goal changes the match score without awarding a goal or assist. The last distinct teammate before the scorer receives an assist, provided no opponent intervened. Repeated scorer contacts retain that teammate; there is no time limit. Spectators cannot affect the match ball. Training balls never affect statistics.

Contacts survive bounces and physical ball replacement, and reset between rounds and on pause. A goal without a known author is safe. Statistics start at zero; past match history is unavailable.

`plugins/CubeBall/clubs.yml` stores versioned UUID profiles, last known names, statistics, memberships, managers and pending transfers. Each mutation is saved by atomic file replacement. Back up this file before manual maintenance and only edit it while the server is stopped. An unreadable or invalid file disables persistent features and logs an error without overwriting it. Free matches remain available. Restore a valid backup and restart to recover. Save failures also disable persistent features rather than reporting a successful write.

## Manual server verification

1. Install the built JAR, restart, and run the example with three players. Confirm the two-versus-one match starts, club names display, and `/cb team` cannot change its teams.
2. Pass from Alice to Carol, let Carol touch repeatedly, then score against red. Check `/cb stats Carol` gives one goal and `/cb stats Alice` one assist. Repeat with a bounce between pass and shot.
3. Intercept with Bob before Carol scores: Carol gets a goal without an assist for Alice. Have Bob score into red's goal: only the match score increases. Test a goal without player contact as well.
4. Generate a separate training ball. A spectator should move that ball but should not move the match ball; training ball contacts must not affect the next scorer or assist.
5. Pause and resume after a pass: the old contact must not receive an assist. Set a short match duration, draw at full time, and verify the deciding overtime goal is recorded once.
6. Accept Carol's transfer during the match, reconnect Carol, and confirm she stays blue. Stop and prepare another club match: she is now red. Confirm a newly added member cannot join the existing frozen squad.
7. Stop or replace a match during a countdown and immediately after a goal. Wait longer than both delays; no old ball or round should appear. Repeat pause/resume during the starting countdown.
8. Set `clubs.max-members: 1`, reload, and check registration and transfer acceptance reject a full destination. Set it back to `0` for unlimited squads. Replace a manager with a pending proposal and confirm its old ID is rejected.
9. Restart with a pending proposal. Check names, clubs, goals, assists and the proposal survive. Accept twice: the second attempt must fail. Delete a club and confirm its former members retain their statistics.
10. On a test server, stop, back up `clubs.yml`, replace it with invalid YAML, and restart. Confirm an error is logged, club commands report unavailability, the file remains unchanged, and a free `/cb match` still works. Restore the backup while stopped.

Automated verification: run `gradlew.bat test build` on Windows or `./gradlew test build` elsewhere. The distributable is `build/libs/CubeBall-2.1.0.jar`. Automated Bukkit tests use mocks; the steps above verify actual server physics and visual behavior.

This release does not include league standings, schedules, aggregate club statistics or a transfer economy.
