package me.fertiz.syncboard.room;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RoomService.class);
    private static final String ID_ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final int ID_LENGTH = 8;

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private final InstantSource clock;
    private final Duration idleTimeout;
    private final int maxRooms;

    public RoomService(InstantSource clock,
                       @Value("${syncboard.room-idle-timeout}") Duration idleTimeout,
                       @Value("${syncboard.max-rooms}") int maxRooms) {
        this.clock = clock;
        this.idleTimeout = idleTimeout;
        this.maxRooms = maxRooms;
    }

    public Optional<Room> create(String code) {
        if (rooms.size() >= maxRooms) {
            return Optional.empty();
        }
        Room room;
        do {
            room = new Room(randomId(), code, clock.instant());
        } while (rooms.putIfAbsent(room.id(), room) != null);
        LOGGER.info("Created room {}{}", room.id(), room.isLocked() ? " with a join code" : "");
        return Optional.of(room);
    }

    public Optional<Room> find(String id) {
        return Optional.ofNullable(rooms.get(id));
    }

    @Scheduled(fixedDelayString = "${syncboard.cleanup-interval}")
    public void removeIdleRooms() {
        Instant now = clock.instant();
        rooms.values().removeIf(room -> {
            synchronized (room) {
                if (!room.isIdle(now, idleTimeout)) {
                    return false;
                }
                room.close();
                LOGGER.info("Removed idle room {}", room.id());
                return true;
            }
        });
    }

    private String randomId() {
        StringBuilder id = new StringBuilder(ID_LENGTH);
        for (int i = 0; i < ID_LENGTH; i++) {
            id.append(ID_ALPHABET.charAt(random.nextInt(ID_ALPHABET.length())));
        }
        return id.toString();
    }
}
