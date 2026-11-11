package me.fertiz.syncboard.board;

import me.fertiz.syncboard.board.dto.request.ClientMessage;
import me.fertiz.syncboard.room.Room;
import me.fertiz.syncboard.room.RoomService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Optional;

@Component
public class BoardSocketHandler extends TextWebSocketHandler {

    private final RoomService rooms;
    private final BoardConnectionRepository connections;
    private final MessageCodec codec;
    private final BoardService board;

    public BoardSocketHandler(RoomService rooms, BoardConnectionRepository connections, MessageCodec codec,
                              BoardService board) {
        this.rooms = rooms;
        this.connections = connections;
        this.codec = codec;
        this.board = board;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        Optional<Room> room = rooms.find(roomId(session.getUri()));
        if (room.isPresent()) {
            connections.save(BoardConnection.open(session, room.get()));
        } else {
            session.close(CloseReason.ROOM_NOT_FOUND.status());
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage text) {
        connections.findById(session.getId()).ifPresent(sender -> receive(sender, text.getPayload()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        connections.removeById(session.getId()).ifPresent(connection -> {
            connection.stop();
            board.leave(connection);
        });
    }

    private void receive(BoardConnection sender, String payload) {
        Optional<ClientMessage> message = codec.read(payload).filter(ClientMessage::isValid);
        if (message.isPresent()) {
            board.handle(sender, message.get());
        } else {
            sender.close(CloseReason.BAD_MESSAGE);
        }
    }

    private static String roomId(URI uri) {
        String path = uri == null ? "" : uri.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
