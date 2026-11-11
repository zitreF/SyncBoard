package me.fertiz.syncboard.room;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomMembersTest {

    private static final Instant NOW = Instant.parse("2026-11-02T12:00:00Z");
    private static final Duration TIMEOUT = Duration.ofMinutes(30);

    private final RoomMembers members = new RoomMembers(NOW);

    @Test
    void givesEveryMemberAnIdAndAColorNobodyElseHas() {
        Member alice = members.join("connection-1", "Alice");
        Member bob = members.join("connection-2", "Bob");
        members.leave("connection-1", NOW);
        Member carol = members.join("connection-3", "Carol");

        assertThat(List.of(alice.id(), bob.id(), carol.id())).containsExactly("m1", "m2", "m3");
        assertThat(alice.color()).isEqualTo(RoomMembers.COLORS.get(0));
        assertThat(bob.color()).isEqualTo(RoomMembers.COLORS.get(1));
        assertThat(carol.color()).isEqualTo(RoomMembers.COLORS.get(0));
        assertThat(members.all()).containsExactly(bob, carol);
        assertThat(members.find("connection-2")).contains(bob);
        assertThat(members.find("connection-1")).isEmpty();
    }

    @Test
    void forgetsSomeoneWhoLeaves() {
        Member alice = members.join("connection-1", "Alice");

        assertThat(members.leave("connection-1", NOW)).contains(alice);
        assertThat(members.leave("connection-1", NOW)).isEmpty();
        assertThat(members.all()).isEmpty();
        assertThat(members.connectionIds()).isEmpty();
    }

    @Test
    void listsTheConnectionsOfItsMembersWithoutLettingThemBeChanged() {
        members.join("connection-1", "Alice");
        members.join("connection-2", "Bob");

        assertThat(members.connectionIds()).containsExactly("connection-1", "connection-2");
        assertThatThrownBy(() -> members.connectionIds().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void isFullAtTheMemberLimit() {
        for (int i = 0; i < RoomMembers.MAX_MEMBERS - 1; i++) {
            members.join("connection-" + i, "Guest");
        }
        assertThat(members.isFull()).isFalse();

        members.join("last", "Guest");

        assertThat(members.isFull()).isTrue();
    }

    @Test
    void hasBeenEmptyOnlyAfterTheWholeTimeout() {
        assertThat(members.hasBeenEmptyFor(TIMEOUT, NOW.plus(TIMEOUT).minusSeconds(1))).isFalse();
        assertThat(members.hasBeenEmptyFor(TIMEOUT, NOW.plus(TIMEOUT))).isTrue();
    }

    @Test
    void countsTheEmptyTimeFromWhenTheLastMemberLeft() {
        Instant later = NOW.plus(Duration.ofHours(2));
        members.join("connection-1", "Alice");
        assertThat(members.hasBeenEmptyFor(TIMEOUT, later)).isFalse();

        members.leave("connection-1", later);

        assertThat(members.hasBeenEmptyFor(TIMEOUT, later.plus(TIMEOUT).minusSeconds(1))).isFalse();
        assertThat(members.hasBeenEmptyFor(TIMEOUT, later.plus(TIMEOUT))).isTrue();
    }

    @Test
    void staysEmptySinceTheSameMomentWhenSomeoneWhoNeverJoinedLeaves() {
        members.leave("never-joined", NOW.plus(TIMEOUT).minusSeconds(1));

        assertThat(members.hasBeenEmptyFor(TIMEOUT, NOW.plus(TIMEOUT))).isTrue();
    }
}
