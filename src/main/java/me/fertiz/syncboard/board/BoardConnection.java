package me.fertiz.syncboard.board;

import me.fertiz.syncboard.room.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

public final class BoardConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(BoardConnection.class);
    private static final int OUTBOX_LIMIT = 1000;

    private final String id;
    private final Room room;
    private final Outbox outbox;

    private BoardConnection(String id, Room room, Outbox outbox) {
        this.id = id;
        this.room = room;
        this.outbox = outbox;
    }

    public static BoardConnection open(WebSocketSession session, Room room) {
        return new BoardConnection(session.getId(), room, new Outbox(session, OUTBOX_LIMIT));
    }

    public String id() {
        return id;
    }

    public Room room() {
        return room;
    }

    public void send(String payload) {
        outbox.send(payload);
    }

    public void close(CloseReason reason) {
        LOGGER.info("Closing {} in room {}: {}", id, room.id(), reason);
        outbox.close(reason);
    }

    public void stop() {
        outbox.stop();
    }
}
