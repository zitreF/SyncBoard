package me.fertiz.syncboard.room;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class RoomRepository {

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    public boolean saveIfAbsent(Room room) {
        return rooms.putIfAbsent(room.id(), room) == null;
    }

    public Optional<Room> findById(String id) {
        return Optional.ofNullable(rooms.get(id));
    }

    public List<Room> findAll() {
        return List.copyOf(rooms.values());
    }

    public void deleteById(String id) {
        rooms.remove(id);
    }

    public long count() {
        return rooms.size();
    }
}
