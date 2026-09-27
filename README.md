<h1 align="center">
    <img src="cube-ball-logo.png" alt="CubeBall" width="800" /><br>
</h1>

<h2 align="center">
    <img src="http://cf.way2muchnoise.eu/full_470085_downloads.svg" alt="Downloads"/>
    <a href="https://github.com/apavarino/Cubeball/actions/workflows/ci.yml"><img src="https://github.com/apavarino/Cubeball/actions/workflows/ci.yml/badge.svg" alt="CI"/></a>
    <a href="https://stellionix.github.io/Cubeball/"><img src="https://img.shields.io/badge/docs-online-blue" alt="Docs"/></a>
    <img src="https://img.shields.io/github/license/apavarino/cubeball" alt="License"/>
    <img src="https://img.shields.io/github/last-commit/apavarino/cubeball" alt="Last commit"/>
</h2>

CubeBall adds football gameplay to Minecraft servers with a physical falling-block ball, team-based matches, arena markers, configurable effects, and in-game admin commands.

It targets Minecraft Java Edition servers running Bukkit-compatible software such as Paper and Spigot.

## Features

- Play football with a configurable falling-block ball
- Create arenas directly in world with block markers for spawns and goals
- Run matches with blue, red, and spectator teams
- Use countdowns, round restarts, score tracking, max goals, and overtime
- Tune particles, sounds, goal animations, timings, and bounce behavior
- Manage everything in game with `/cb`
- Track persistent goals and assists with `/cb stats [player]`
- Create clubs, appoint managers, agree transfers and play club matches

See [club commands](docs/commands-and-perms.md) and the [complete club match example and server checklist](docs/club-matches.md).

## Compatibility

- Minecraft Java Edition
- Bukkit API `1.13+`
- Recommended server software: Paper
- Also intended for Bukkit, Spigot, and compatible forks

## Download

- [CurseForge](https://www.curseforge.com/minecraft/bukkit-plugins/cubeball)
- [Bukkit Dev](https://dev.bukkit.org/projects/cubeball)
- [GitHub repository](https://github.com/apavarino/Cubeball)

## Quick Start

1. Download the latest CubeBall JAR.
2. Put it in the server `plugins/` folder.
3. Start the server once to generate `config.yml`.
4. Build a field and place the arena marker blocks.
5. Run `/cb match` near the arena.
6. Assign teams with `/cb team <BLUE|RED|SPECTATOR> <player>`.
7. Start the game with `/cb start`.

## Arena Markers

Default materials:

- Ball spawn: `EMERALD_BLOCK`
- Blue spawn: `BLUE_WOOL`
- Red spawn: `RED_WOOL`
- Blue goal: `BLUE_CONCRETE`
- Red goal: `RED_CONCRETE`

These markers can be changed in `config.yml`.

## Commands

Main command:

```text
/cb
```

Common examples:

- `/cb reload`
- `/cb match`
- `/cb start`
- `/cb pause`
- `/cb resume`
- `/cb stop`
- `/cb team BLUE PlayerName`
- `/cb generate training-ball`
- `/cb remove training-ball`

Permission:

- `cubeball.manage`

## Documentation

The complete documentation is published at [stellionix.github.io/Cubeball](https://stellionix.github.io/Cubeball/).

Useful pages:

- [Installation](https://stellionix.github.io/Cubeball/installation/)
- [Arena Setup](https://stellionix.github.io/Cubeball/arena-setup/)
- [Configuration](https://stellionix.github.io/Cubeball/configuration/)
- [Commands and Permissions](https://stellionix.github.io/Cubeball/commands-and-perms/)
- [Troubleshooting](https://stellionix.github.io/Cubeball/troubleshooting/)

## Build From Source

```bash
./gradlew test shadowJar
```

The plugin artifact is generated in `build/libs/`.

## Contributing

Technical contribution guidelines are available in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

This project is distributed under the [LICENSE](LICENSE).
