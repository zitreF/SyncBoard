package me.fertiz.syncboard.room;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.InstantSource;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class RoomService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomService.class);

    private final RoomRepository repository;
    private final RoomIdGenerator ids;
    private final RoomProperties properties;
    private final InstantSource clock;

    public RoomService(RoomRepository repository, RoomIdGenerator ids, RoomProperties properties, InstantSource clock) {
        this.repository = repository;
        this.ids = ids;
        this.properties = properties;
        this.clock = clock;
    }

    public Optional<Room> create(String code) {
        if (repository.count() >= properties.maxRooms()) {
            return Optional.empty();
        }
        Room room = new Room(ids.next(), code, clock.instant());
        while (!repository.saveIfAbsent(room)) {
            room = new Room(ids.next(), code, clock.instant());
        }
        LOGGER.info("Created room {}{}", room.id(), room.isLocked() ? " with a join code" : "");
        return Optional.of(room);
    }

    public Optional<Room> find(String id) {
        return repository.findById(id);
    }

    @Scheduled(fixedDelayString = "${syncboard.cleanup-interval}")
    public void removeIdleRooms() {
        Instant now = clock.instant();
        CompletableFuture<?>[] removals = repository.findAll().stream()
                .map(room -> removeIfIdle(room, now))
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(removals).join();
    }

    private CompletableFuture<Void> removeIfIdle(Room room, Instant now) {
        return room.closeIfIdle(now, properties.roomIdleTimeout()).thenAccept(closed -> {
            if (closed) {
                repository.deleteById(room.id());
                LOGGER.info("Removed idle room {}", room.id());
            }
        });
    }
}
