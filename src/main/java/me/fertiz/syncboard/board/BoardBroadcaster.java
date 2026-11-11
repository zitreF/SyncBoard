package me.fertiz.syncboard.board;

import me.fertiz.syncboard.board.dto.response.ServerMessage;
import me.fertiz.syncboard.room.RoomMembers;
import org.springframework.stereotype.Component;

@Component
public class BoardBroadcaster {

    private final BoardConnectionRepository connections;
    private final MessageCodec codec;

    public BoardBroadcaster(BoardConnectionRepository connections, MessageCodec codec) {
        this.connections = connections;
        this.codec = codec;
    }

    public void send(BoardConnection recipient, ServerMessage message) {
        recipient.send(codec.write(message));
    }

    public void sendToEveryone(RoomMembers members, ServerMessage message) {
        String payload = codec.write(message);
        for (String connectionId : members.connectionIds()) {
            deliver(connectionId, payload);
        }
    }

    public void sendToOthers(RoomMembers members, BoardConnection sender, ServerMessage message) {
        String payload = codec.write(message);
        for (String connectionId : members.connectionIds()) {
            if (!connectionId.equals(sender.id())) {
                deliver(connectionId, payload);
            }
        }
    }

    private void deliver(String connectionId, String payload) {
        connections.findById(connectionId).ifPresent(connection -> connection.send(payload));
    }
}
