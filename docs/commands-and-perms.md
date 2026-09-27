# Commands And Permissions

The main command is `/cb`.

## Commands

`/cb reload`
: Reloads `config.yml`. Available from console and to players with `cubeball.manage`.

`/cb match`
: Scans the nearby arena markers and prepares a match.

`/cb start`
: Starts the configured match and launches the countdown.

`/cb stop`
: Cancels the current match.

`/cb pause`
: Pauses the current match and removes the active ball.

`/cb resume`
: Restarts a paused match with a new countdown.

`/cb team <BLUE|RED|SPECTATOR> <player>`
: Assigns a player to a team in a free match. Club match compositions cannot be edited with this command.

`/cb generate <id>`
: Spawns a ball at the executing player's location.

`/cb generate <id> <x> <y> <z> <world>`
: Spawns a ball at an explicit position.

`/cb remove <id>`
: Removes a tracked ball by identifier.

## Permission

`cubeball.manage`
: Grants access to management commands. Defaults to server operators.

## Notes

- Console supports `reload`, `generate`, `remove`, statistics, club consultation and club administration. Arena preparation requires an in-game player. Transfer decisions require the relevant manager's player identity.
- `/cb` or `/cb help` displays help adapted to your role. Tab completion filters commands and transfer IDs according to your permissions and manager role.

## Statistics and clubs

| Command | Access | Purpose |
| --- | --- | --- |
| `/cb stats [player]` | Everyone | Goals, assists and current club; console must specify a player |
| `/cb club list` | Everyone | List club IDs and names |
| `/cb club info <id>` | Everyone | Show manager and members |
| `/cb club create <id> <name...>` | `cubeball.manage` | Create a club with a display name containing spaces |
| `/cb club delete <id>` | `cubeball.manage` | Delete a club, preserving individual statistics |
| `/cb club manager <id> <player>` | `cubeball.manage` | Set or replace the manager |
| `/cb club add <id> <player>` | `cubeball.manage` | Register a player who has no club |
| `/cb club remove <id> <player>` | `cubeball.manage` | Remove a member |
| `/cb club transfer <player> <destination>` | Source manager | Propose a transfer |
| `/cb club transfers` | Managers; console | Show your pending proposals; console lists all |
| `/cb club accept <transfer>` | Destination manager | Accept a proposal |
| `/cb club reject <transfer>` | Destination manager | Reject a proposal |
| `/cb club cancel <transfer>` | Source manager | Cancel a proposal |
| `/cb match <blueClub> <redClub>` | `cubeball.manage`, in game | Scan the arena and prepare a club match |

Club IDs are case-insensitive and use letters, digits, underscores or hyphens. Player arguments accept known names or UUIDs, including offline players known to the server. Players have at most one club. Assigning a manager does not add them to the squad, and a manager does not need `cubeball.manage` to handle transfers.

Only one proposal per player may be pending. Acceptance rechecks the managers, membership and destination capacity. Manager changes, club deletion and membership changes invalidate affected proposals. A full destination leaves the proposal pending so its manager can retry after freeing a place. Transfers never move or reset individual statistics.

See [Club matches and server verification](club-matches.md) for a complete example.
