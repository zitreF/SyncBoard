package me.fertiz.syncboard.room;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoomServiceTest {

    private Instant now = Instant.parse("2026-11-02T12:00:00Z");
    private final RoomService service = new RoomService(() -> now, Duration.ofMinutes(30), 50);

    @Test
    void createsRoomsWithShortRandomIds() {
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            Room room = service.create(null).orElseThrow();
            assertThat(room.id()).matches("[a-z0-9]{8}");
            assertThat(service.find(room.id())).containsSame(room);
            ids.add(room.id());
        }
        assertThat(ids).hasSize(50);
    }

    @Test
    void refusesNewRoomsAtTheLimit() {
        for (int i = 0; i < 50; i++) {
            service.create(null);
        }

        assertThat(service.create(null)).isEmpty();
    }

    @Test
    void removesRoomsNobodyUsedForTheTimeout() {
        Room room = service.create("secret-word").orElseThrow();

        now = now.plus(Duration.ofMinutes(29));
        service.removeIdleRooms();
        assertThat(service.find(room.id())).isPresent();

        now = now.plus(Duration.ofMinutes(1));
        service.removeIdleRooms();
        assertThat(service.find(room.id())).isEmpty();
        assertThat(room.isClosed()).isTrue();
    }

    @Test
    void keepsRoomsWithPeopleInThem() {
        Room room = service.create(null).orElseThrow();
        room.join("session-1", "Alice");

        now = now.plus(Duration.ofHours(5));
        service.removeIdleRooms();

        assertThat(service.find(room.id())).isPresent();
        assertThat(room.isClosed()).isFalse();
    }
}
