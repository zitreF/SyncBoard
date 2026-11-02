package me.fertiz.syncboard.socket;

import me.fertiz.syncboard.room.Member;
import me.fertiz.syncboard.room.Room;
import me.fertiz.syncboard.room.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.time.InstantSource;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BoardSocketHandler extends TextWebSocketHandler {

    static final CloseStatus BAD_MESSAGE = new CloseStatus(4400, "Unexpected message");
    static final CloseStatus WRONG_CODE = new CloseStatus(4403, "Wrong join code");
    static final CloseStatus ROOM_NOT_FOUND = new CloseStatus(4404, "This board does not exist");
    static final CloseStatus ROOM_FULL = new CloseStatus(4409, "This board is full");

    private static final Logger LOGGER = LoggerFactory.getLogger(BoardSocketHandler.class);
    private static final int OUTBOX_LIMIT = 1000;

    private record Connection(String id, Room room, Outbox outbox) {}

    private final RoomService rooms;
    private final JsonMapper json;
    private final InstantSource clock;
    private final Map<String, Connection> connections = new ConcurrentHashMap<>();

    public BoardSocketHandler(RoomService rooms, JsonMapper json, InstantSource clock) {
        this.rooms = rooms;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        Optional<Room> room = rooms.find(roomId(session.getUri()));
        if (room.isEmpty()) {
            session.close(ROOM_NOT_FOUND);
            return;
        }
        connections.put(session.getId(), new Connection(session.getId(), room.get(), new Outbox(session, OUTBOX_LIMIT)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage text) {
        Connection connection = connections.get(session.getId());
        if (connection == null) {
            return;
        }
        ClientMessage message;
        try {
            message = json.readValue(text.getPayload(), ClientMessage.class);
        } catch (JacksonException e) {
            reject(connection, BAD_MESSAGE);
            return;
        }
        inRoom(connection, () -> {
            Member member = connection.room().member(connection.id());
            if (member != null) {
                handle(connection, member, message);
            } else if (message instanceof ClientMessage.Join join) {
                join(connection, join);
            } else {
                reject(connection, BAD_MESSAGE);
            }
        });
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Connection connection = connections.remove(session.getId());
        if (connection == null) {
            return;
        }
        connection.outbox().stop();
        Room room = connection.room();
        inRoom(connection, () -> {
            if (room.leave(connection.id(), clock.instant()) != null) {
                broadcast(room, new ServerMessage.Members(room.members()), null);
            }
        });
    }

    private void inRoom(Connection connection, Runnable task) {
        connection.room().execute(task).exceptionally(error -> {
            LOGGER.warn("Room {} could not handle a message from {}", connection.room().id(), connection.id(), error);
            connection.outbox().close(CloseStatus.SERVER_ERROR);
            return null;
        });
    }

    private void join(Connection connection, ClientMessage.Join join) {
        Room room = connection.room();
        if (room.isClosed()) {
            reject(connection, ROOM_NOT_FOUND);
        } else if (!room.accepts(join.code())) {
            reject(connection, WRONG_CODE);
        } else if (room.isFull()) {
            reject(connection, ROOM_FULL);
        } else {
            Member member = room.join(connection.id(), join.name());
            send(connection, new ServerMessage.Welcome(member.id(), room.members(), room.strokes()));
            broadcast(room, new ServerMessage.Members(room.members()), connection.id());
        }
    }

    private void handle(Connection connection, Member member, ClientMessage message) {
        Room room = connection.room();
        switch (message) {
            case ClientMessage.Draw draw when draw.isValid() -> {
                if (room.draw(draw.id(), draw.color(), draw.width(), draw.points())) {
                    broadcast(room, new ServerMessage.Draw(draw.id(), draw.color(), draw.width(), draw.points()), connection.id());
                } else {
                    room.erase(draw.id());
                    broadcast(room, new ServerMessage.Erase(draw.id()), null);
                    send(connection, new ServerMessage.Notice("The board is full, clear something to keep drawing"));
                }
            }
            case ClientMessage.Erase(String id) -> {
                if (room.erase(id)) {
                    broadcast(room, new ServerMessage.Erase(id), connection.id());
                }
            }
            case ClientMessage.Clear() -> {
                if (room.clear()) {
                    broadcast(room, new ServerMessage.Clear(member.id()), connection.id());
                }
            }
            case ClientMessage.Cursor cursor when cursor.isValid() ->
                    broadcast(room, new ServerMessage.Cursor(member.id(), cursor.x(), cursor.y()), connection.id());
            default -> reject(connection, BAD_MESSAGE);
        }
    }

    private void send(Connection connection, ServerMessage message) {
        connection.outbox().send(json.writeValueAsString(message));
    }

    private void broadcast(Room room, ServerMessage message, String exceptId) {
        String payload = json.writeValueAsString(message);
        for (String id : room.sessionIds()) {
            Connection connection = connections.get(id);
            if (connection != null && !id.equals(exceptId)) {
                connection.outbox().send(payload);
            }
        }
    }

    private void reject(Connection connection, CloseStatus status) {
        LOGGER.info("Closing {} in room {}: {}", connection.id(), connection.room().id(), status.getReason());
        connection.outbox().close(status);
    }

    private static String roomId(URI uri) {
        String path = uri == null ? "" : uri.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
