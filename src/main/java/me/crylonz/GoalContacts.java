package me.crylonz;

import java.util.UUID;

/** Logical contacts survive replacement of the physical ball entity. */
final class GoalContacts {
    record Credit(UUID scorer, UUID assistant) {}
    private UUID last, previous;
    private Team side;

    void touch(UUID player, Team team) {
        if (player == null || team == Team.SPECTATOR) return;
        if (player.equals(last) && team == side) return;
        previous = team == side ? last : null;
        last = player; side = team;
    }
    Credit credit(Team scoringSide) {
        return side == scoringSide ? new Credit(last, previous) : new Credit(null, null);
    }
    void reset() { last = null; previous = null; side = null; }
}
