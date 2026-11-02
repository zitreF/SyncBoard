package me.fertiz.syncboard.room;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomTest {

    private static final Instant NOW = Instant.parse("2026-11-02T12:00:00Z");
    private static final Duration TIMEOUT = Duration.ofMinutes(30);

    private final Room room = new Room("abcd2345", null, NOW);

    @Test
    void appendsPointsToAStrokeAndKeepsItsFirstStyle() {
        assertThat(room.draw("s1", "#e03131", 4, new int[]{1, 2, 3, 4})).isTrue();
        assertThat(room.draw("s1", "#1971c2", 8, new int[]{5, 6})).isTrue();

        assertThat(room.strokes()).containsExactly(new Stroke("s1", "#e03131", 4, List.of(1, 2, 3, 4, 5, 6)));
    }

    @Test
    void keepsStrokesInTheOrderTheyWereStarted() {
        room.draw("s1", "#1e1e1e", 2, new int[]{1, 1});
        room.draw("s2", "#1e1e1e", 2, new int[]{2, 2});
        room.draw("s1", "#1e1e1e", 2, new int[]{3, 3});

        assertThat(room.strokes()).extracting(Stroke::id).containsExactly("s1", "s2");
    }

    @Test
    void handsOutCopiesThatCannotChangeTheRoom() {
        room.draw("s1", "#1e1e1e", 2, new int[]{1, 1});
        List<Stroke> copy = room.strokes();

        assertThatThrownBy(() -> copy.get(0).points().add(9)).isInstanceOf(UnsupportedOperationException.class);
        room.draw("s1", "#1e1e1e", 2, new int[]{2, 2});

        assertThat(copy.get(0).points()).containsExactly(1, 1);
        assertThat(room.strokes().get(0).points()).containsExactly(1, 1, 2, 2);
    }

    @Test
    void erasesAndClearsStrokes() {
        room.draw("s1", "#1e1e1e", 2, new int[]{1, 1});
        room.draw("s2", "#1e1e1e", 2, new int[]{2, 2});

        assertThat(room.erase("s1")).isTrue();
        assertThat(room.erase("s1")).isFalse();
        assertThat(room.strokes()).extracting(Stroke::id).containsExactly("s2");
        assertThat(room.clear()).isTrue();
        assertThat(room.clear()).isFalse();
        assertThat(room.strokes()).isEmpty();
    }

    @Test
    void refusesStrokesOverTheLimits() {
        for (int i = 0; i < Room.MAX_STROKES; i++) {
            assertThat(room.draw("s" + i, "#1e1e1e", 2, new int[]{i, i})).isTrue();
        }
        assertThat(room.draw("one-too-many", "#1e1e1e", 2, new int[]{0, 0})).isFalse();

        room.clear();
        assertThat(room.draw("long", "#1e1e1e", 2, new int[Room.MAX_STROKE_COORDINATES])).isTrue();
        assertThat(room.draw("long", "#1e1e1e", 2, new int[]{0, 0})).isFalse();
    }

    @Test
    void checksTheJoinCode() {
        Room locked = new Room("abcd2345", "secret-word", NOW);

        assertThat(room.isLocked()).isFalse();
        assertThat(room.accepts(null)).isTrue();
        assertThat(locked.isLocked()).isTrue();
        assertThat(locked.accepts("secret-word")).isTrue();
        assertThat(locked.accepts("Secret-word")).isFalse();
        assertThat(locked.accepts("")).isFalse();
        assertThat(locked.accepts(null)).isFalse();
    }

    @Test
    void givesEveryMemberAnIdAndAColorNobodyElseHas() {
        Member alice = room.join("session-1", "Alice");
        Member bob = room.join("session-2", "Bob");
        room.leave("session-1", NOW);
        Member carol = room.join("session-3", "Carol");

        assertThat(List.of(alice.id(), bob.id(), carol.id())).containsExactly("m1", "m2", "m3");
        assertThat(alice.color()).isEqualTo(Room.COLORS.get(0));
        assertThat(bob.color()).isEqualTo(Room.COLORS.get(1));
        assertThat(carol.color()).isEqualTo(Room.COLORS.get(0));
        assertThat(room.members()).containsExactly(bob, carol);
        assertThat(room.member("session-2")).isEqualTo(bob);
        assertThat(room.member("session-1")).isNull();
    }

    @Test
    void isFullAtTheMemberLimit() {
        for (int i = 0; i < Room.MAX_MEMBERS - 1; i++) {
            room.join("session-" + i, "Guest");
        }
        assertThat(room.isFull()).isFalse();

        room.join("last", "Guest");

        assertThat(room.isFull()).isTrue();
    }

    @Test
    void cleansNames() {
        assertThat(Member.cleanName("  Bob\tthe​  builder\n")).isEqualTo("Bob the builder");
        assertThat(Member.cleanName("‮​")).isEqualTo("Guest");
        assertThat(Member.cleanName(null)).isEqualTo("Guest");
        assertThat(Member.cleanName("x".repeat(50))).hasSize(Member.MAX_NAME_LENGTH);
    }

    @Test
    void becomesIdleOnlyAfterBeingEmptyForTheWholeTimeout() {
        assertThat(room.isIdle(NOW.plus(TIMEOUT).minusSeconds(1), TIMEOUT)).isFalse();
        assertThat(room.isIdle(NOW.plus(TIMEOUT), TIMEOUT)).isTrue();

        room.join("session-1", "Alice");
        Instant later = NOW.plus(Duration.ofHours(2));
        assertThat(room.isIdle(later, TIMEOUT)).isFalse();

        room.leave("session-1", later);
        assertThat(room.isIdle(later.plus(TIMEOUT).minusSeconds(1), TIMEOUT)).isFalse();
        assertThat(room.isIdle(later.plus(TIMEOUT), TIMEOUT)).isTrue();
    }
}
