package me.crylonz;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ClubStoreTest {
    @TempDir Path directory;
    ClubStore store;
    UUID alice = UUID.randomUUID(), bob = UUID.randomUUID(), player = UUID.randomUUID();
    @BeforeEach void setup() {
        store = open();
        store.remember(alice, "Alice"); store.remember(bob, "Bob"); store.remember(player, "Player");
        store.create("blue", "Blue Club"); store.create("red", "Red Club");
        store.manager("blue", alice); store.manager("red", bob); store.add("blue", player);
    }
    ClubStore open() { return new ClubStore(directory.resolve("clubs.yml"), 0, s -> {}); }

    @Test void restartRenameTransferAndDeletionPreserveStats() {
        store.recordGoal(player, alice);
        ClubStore.Transfer transfer = store.propose(alice, player, "red");
        store = open(); assertTrue(store.available()); assertEquals(1, store.transfers().size());
        store.remember(player, "NewName"); store.resolve(transfer.id(), bob, "accept");
        store = open(); assertEquals("red", store.membership(player)); assertEquals("NewName", store.profile(player).name());
        assertEquals(1, store.profile(player).goals()); assertEquals(1, store.profile(alice).assists());
        store.delete("red"); store = open();
        assertNull(store.membership(player)); assertEquals(1, store.profile(player).goals());
    }
    @Test void consentCapacityAndDoubleAcceptance() {
        assertThrows(IllegalArgumentException.class, () -> store.propose(bob, player, "red"));
        var t = store.propose(alice, player, "red");
        assertThrows(IllegalArgumentException.class, () -> store.propose(alice, player, "red"));
        assertThrows(IllegalArgumentException.class, () -> store.resolve(t.id(), alice, "accept"));
        store.setMaxMembers(1); store.add("red", bob);
        assertThrows(IllegalArgumentException.class, () -> store.resolve(t.id(), bob, "accept"));
        assertEquals("blue", store.membership(player));
        store.remove("red", bob); store.resolve(t.id(), bob, "accept");
        assertThrows(IllegalArgumentException.class, () -> store.resolve(t.id(), bob, "accept"));
    }
    @Test void membershipAndManagersInvalidatePendingTransfers() {
        var t = store.propose(alice, player, "red"); store.manager("blue", bob);
        assertTrue(store.transfers().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> store.resolve(t.id(), bob, "accept"));
        store.manager("blue", alice); store.propose(alice, player, "red"); store.remove("blue", player);
        assertTrue(store.transfers().isEmpty());
        store.add("blue", player); store.propose(alice, player, "red"); store.delete("red");
        assertTrue(store.transfers().isEmpty());
    }
    @Test void managersAreNotMembersAndRejectionAndCancellationRequireCorrectActor() {
        assertNull(store.membership(alice));
        var t = store.propose(alice, player, "red");
        assertThrows(IllegalArgumentException.class, () -> store.resolve(t.id(), bob, "cancel"));
        store.resolve(t.id(), alice, "cancel");
        var second = store.propose(alice, player, "red"); store.resolve(second.id(), bob, "reject");
        assertTrue(store.transfers().isEmpty()); assertEquals("blue", store.membership(player));
        assertThrows(IllegalArgumentException.class, () -> store.add("red", player));
    }
    @Test void invalidFilesDisableWithoutOverwriting() throws Exception {
        for (String content : List.of("broken: [", "version: 99", "version: 1\nprofiles: {}\nclubs: []\ntransfers: {}")) {
            Files.writeString(directory.resolve("clubs.yml"), content);
            ClubStore invalid = open(); assertFalse(invalid.available());
            assertThrows(IllegalStateException.class, () -> invalid.remember(player, "Overwrite"));
            assertEquals(content, Files.readString(directory.resolve("clubs.yml")));
        }
    }
    @Test void failedSavePreservesPreviousFileAndDisablesService() throws Exception {
        Path parentFile = directory.resolve("not-a-directory"); Files.writeString(parentFile, "original");
        ClubStore failing = new ClubStore(parentFile.resolve("clubs.yml"), 0, s -> {});
        assertThrows(IllegalStateException.class, () -> failing.remember(player, "Player"));
        assertFalse(failing.available()); assertEquals("original", Files.readString(parentFile));
    }
}
