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
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.SessionLimitExceededException;
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
    private static final int SEND_TIME_LIMIT = 5_000;
    private static final int SEND_BUFFER_LIMIT = 1024 * 1024;

    private record Connection(WebSocketSession session, Room room) {}

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
        WebSocketSession safe = new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT, SEND_BUFFER_LIMIT);
        connections.put(session.getId(), new Connection(safe, room.get()));
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
            close(connection, BAD_MESSAGE);
            return;
        }
        Room room = connection.room();
        synchronized (room) {
            Member member = room.member(session.getId());
            if (member != null) {
                handle(connection, member, message);
            } else if (message instanceof ClientMessage.Join join) {
                join(connection, join);
            } else {
                close(connection, BAD_MESSAGE);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Connection connection = connections.remove(session.getId());
        if (connection == null) {
            return;
        }
        Room room = connection.room();
        synchronized (room) {
            if (room.leave(session.getId(), clock.instant()) != null) {
                broadcast(room, new ServerMessage.Members(room.members()), null);
            }
        }
    }

    private void join(Connection connection, ClientMessage.Join join) {
        Room room = connection.room();
        if (room.isClosed()) {
            close(connection, ROOM_NOT_FOUND);
        } else if (!room.accepts(join.code())) {
            close(connection, WRONG_CODE);
        } else if (room.isFull()) {
            close(connection, ROOM_FULL);
        } else {
            String sessionId = connection.session().getId();
            Member member = room.join(sessionId, join.name());
            send(connection, new ServerMessage.Welcome(member.id(), room.members(), room.strokes()));
            broadcast(room, new ServerMessage.Members(room.members()), sessionId);
        }
    }

    private void handle(Connection connection, Member member, ClientMessage message) {
        Room room = connection.room();
        String sessionId = connection.session().getId();
        switch (message) {
            case ClientMessage.Draw draw when draw.isValid() -> {
                if (room.draw(draw.id(), draw.color(), draw.width(), draw.points())) {
                    broadcast(room, new ServerMessage.Draw(draw.id(), draw.color(), draw.width(), draw.points()), sessionId);
                } else {
                    room.erase(draw.id());
                    broadcast(room, new ServerMessage.Erase(draw.id()), null);
                    send(connection, new ServerMessage.Notice("The board is full, clear something to keep drawing"));
                }
            }
            case ClientMessage.Erase(String id) -> {
                if (room.erase(id)) {
                    broadcast(room, new ServerMessage.Erase(id), sessionId);
                }
            }
            case ClientMessage.Clear() -> {
                if (room.clear()) {
                    broadcast(room, new ServerMessage.Clear(member.id()), sessionId);
                }
            }
            case ClientMessage.Cursor cursor when cursor.isValid() ->
                    broadcast(room, new ServerMessage.Cursor(member.id(), cursor.x(), cursor.y()), sessionId);
            default -> close(connection, BAD_MESSAGE);
        }
    }

    private void send(Connection connection, ServerMessage message) {
        deliver(connection.session(), json.writeValueAsString(message));
    }

    private void broadcast(Room room, ServerMessage message, String exceptSessionId) {
        String payload = json.writeValueAsString(message);
        for (String sessionId : room.sessionIds()) {
            Connection connection = connections.get(sessionId);
            if (connection != null && !sessionId.equals(exceptSessionId)) {
                deliver(connection.session(), payload);
            }
        }
    }

    private void deliver(WebSocketSession session, String payload) {
        try {
            session.sendMessage(new TextMessage(payload));
        } catch (IOException | SessionLimitExceededException e) {
            LOGGER.debug("Could not send to {}: {}", session.getId(), e.getMessage());
        }
    }

    private void close(Connection connection, CloseStatus status) {
        LOGGER.info("Closing {} in room {}: {}", connection.session().getId(), connection.room().id(), status.getReason());
        try {
            connection.session().close(status);
        } catch (IOException e) {
            LOGGER.debug("Could not close {}: {}", connection.session().getId(), e.getMessage());
        }
    }

    private static String roomId(URI uri) {
        String path = uri == null ? "" : uri.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
