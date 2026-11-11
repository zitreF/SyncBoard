package me.fertiz.syncboard.board;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class BoardConnectionRepository {

    private final Map<String, BoardConnection> connections = new ConcurrentHashMap<>();

    public void save(BoardConnection connection) {
        connections.put(connection.id(), connection);
    }

    public Optional<BoardConnection> findById(String id) {
        return Optional.ofNullable(connections.get(id));
    }

    public Optional<BoardConnection> removeById(String id) {
        return Optional.ofNullable(connections.remove(id));
    }
}
