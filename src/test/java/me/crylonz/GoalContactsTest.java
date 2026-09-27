package me.crylonz;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class GoalContactsTest {
    UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
    GoalContacts contacts = new GoalContacts();
    @Test void repeatedScorerContactsKeepLastDistinctTeammate() {
        contacts.touch(a, Team.BLUE); contacts.touch(b, Team.BLUE); contacts.touch(b, Team.BLUE);
        assertEquals(new GoalContacts.Credit(b, a), contacts.credit(Team.BLUE));
        contacts.touch(c, Team.BLUE); assertEquals(new GoalContacts.Credit(c, b), contacts.credit(Team.BLUE));
    }
    @Test void interceptionClearsAssistAndOwnGoalHasNoCredit() {
        contacts.touch(a, Team.BLUE); contacts.touch(c, Team.RED); contacts.touch(b, Team.BLUE);
        assertEquals(new GoalContacts.Credit(b, null), contacts.credit(Team.BLUE));
        assertNull(contacts.credit(Team.RED).scorer());
    }
    @Test void noAuthorResetAndSpectatorAreSafe() {
        assertNull(contacts.credit(Team.BLUE).scorer());
        contacts.touch(a, Team.BLUE); contacts.touch(c, Team.SPECTATOR);
        assertEquals(a, contacts.credit(Team.BLUE).scorer());
        contacts.reset(); assertNull(contacts.credit(Team.BLUE).scorer());
    }
}
