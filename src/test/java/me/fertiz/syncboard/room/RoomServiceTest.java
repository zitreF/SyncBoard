package me.fertiz.syncboard.room;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RoomServiceTest {

    private Instant now = Instant.parse("2026-11-02T12:00:00Z");
    private final RoomProperties properties = new RoomProperties(50, Duration.ofMinutes(30));
    private final RoomService service =
            new RoomService(new RoomRepository(), new RoomIdGenerator(), properties, () -> now);

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
        assertThat(room.execute(state -> state.drawing().clear())).isCompletedExceptionally();
    }

    @Test
    void keepsRoomsWithPeopleInThem() {
        Room room = service.create(null).orElseThrow();
        room.execute(state -> state.members().join("connection-1", "Alice")).join();

        now = now.plus(Duration.ofHours(5));
        service.removeIdleRooms();

        assertThat(service.find(room.id())).isPresent();
        assertThat(room.execute(state -> state.drawing().clear())).succeedsWithin(Duration.ofSeconds(2));
    }
}
