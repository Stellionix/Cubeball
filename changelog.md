# Changelog

## 2.1.0 - 2026-09-27

### Added

- Persistent player statistics with `/cb stats [player]`: goals, assists and current club, including known offline players. Profiles use UUIDs and retain the latest known player name.
- Club administration with `/cb club create`, `delete`, `manager`, `add` and `remove`. Clubs have a unique ID and a display name that can contain spaces; each player can belong to only one club.
- Public club listings and squad details through `/cb club list` and `/cb club info <id>`. Statistics, club consultation and administration are also available from the console.
- Club managers can propose, list, accept, reject and cancel transfers without administrative permissions. Transfers require agreement from the source and destination managers, with one pending proposal per player.
- Transfer acceptance rechecks the managers, player membership and destination capacity. Manager changes, membership changes and club deletion invalidate affected proposals. Individual statistics are preserved after transfers and club deletion.
- Configurable club capacity through `clubs.max-members`, with `0` providing unlimited membership. Assigning a manager does not automatically register them as a squad member.
- Club matches through `/cb match <blueClub> <redClub>`, with club names displayed and connected members assigned automatically. Starting requires at least one connected member per club and permits unequal squads.
- Club compositions are refreshed at kickoff and remain fixed for the encounter. Reconnecting players recover their side; transfers affect subsequent matches. `/cb team` remains available for free matches only.
- Versioned YAML storage for profiles, clubs and pending transfers, saved through atomic file replacement. Invalid data disables persistent features without overwriting the file; free matches remain available.
- Role-aware command help and tab completion, including club IDs, known players and pending transfer IDs.
- Documentation covering club administration, transfers, configuration and a complete club match example.

### Fixed

- Goal attribution uses the last player touching the match ball, provided their side scores. Own goals increase only the match score; goals without an identified scorer no longer cause an error.
- Assists go to the last distinct teammate before the scorer, without an intervening opponent contact. Repeated scorer contacts preserve the assist, while an interception clears it.
- Contact history survives rebounds and replacement of the physical ball, and resets between rounds and on pause. Training ball contacts cannot affect match statistics.
- Spectators can no longer interact with the match ball. Ball rebounds identify the exact tracked entity instead of selecting a nearby ball.
- Stopping or replacing a match cancels its delayed countdowns and round restarts and removes its ball, preventing old rounds from restarting afterward.
- Match shutdown cleans up tracked balls, and ball updates tolerate removal during goal handling. Goal detection also checks the world containing the goal.
- Resuming a paused match immediately leaves the paused state, preventing repeated resume commands from scheduling duplicate countdowns.
- The match ball's reserved ID cannot be used by manual ball generation or removal commands.
- Goal effects accept both `BONE_MEAL_USE` and the legacy `VILLAGER_PLANT_GROW` name, selecting the equivalent supported by the server. This fixes startup failures on newer Paper versions while retaining support for older servers.
- Unknown goal effect names log a warning and fall back to the default instead of blocking startup. If neither default name is available, the effect is skipped.
- Gradle resource processing now tracks the project version, ensuring the version inside `plugin.yml` updates when building a new release.

### Changed

- Plugin version updated to `2.1.0`.
- New configurations use `BONE_MEAL_USE` for goal celebrations; existing configurations using the legacy name remain supported.
- Goal and assist counters begin at zero; historical goals from earlier versions cannot be reconstructed.
