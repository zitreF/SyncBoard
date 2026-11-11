package me.fertiz.syncboard.board;

import me.fertiz.syncboard.board.dto.request.ClearMessage;
import me.fertiz.syncboard.board.dto.request.JoinMessage;
import me.fertiz.syncboard.room.Room;
import me.fertiz.syncboard.room.RoomIdGenerator;
import me.fertiz.syncboard.room.RoomProperties;
import me.fertiz.syncboard.room.RoomRepository;
import me.fertiz.syncboard.room.RoomService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BoardServiceTest {

    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);
    private static final JoinMessage JOIN = new JoinMessage("Alice", null);

    private Instant now = Instant.parse("2026-11-02T12:00:00Z");
    private final InstantSource clock = () -> now;
    private final RoomProperties properties = new RoomProperties(10, IDLE_TIMEOUT);
    private final RoomService rooms = new RoomService(new RoomRepository(), new RoomIdGenerator(), properties, clock);
    private final BoardConnectionRepository connections = new BoardConnectionRepository();
    private final MessageCodec codec = new MessageCodec(JsonMapper.builder().build());
    private final BoardService board = new BoardService(new BoardBroadcaster(connections, codec), clock);
    private final WebSocketSession session = mock(WebSocketSession.class);

    @BeforeEach
    void setUp() {
        when(session.getId()).thenReturn("connection-1");
    }

    @Test
    void welcomesSomeoneWhoJoins() throws Exception {
        BoardConnection connection = connectTo(rooms.create(null).orElseThrow());

        board.handle(connection, JOIN);

        ArgumentCaptor<TextMessage> sent = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, timeout(2000)).sendMessage(sent.capture());
        assertThat(sent.getValue().getPayload()).contains("\"type\":\"welcome\"", "\"name\":\"Alice\"");
        verify(session, never()).close(any());
    }

    @Test
    void refusesAnythingButJoinFromSomeoneWhoHasNotJoined() throws Exception {
        BoardConnection connection = connectTo(rooms.create(null).orElseThrow());

        board.handle(connection, new ClearMessage());

        verify(session, timeout(2000)).close(CloseReason.BAD_MESSAGE.status());
    }

    @Test
    void refusesAWrongJoinCode() throws Exception {
        BoardConnection connection = connectTo(rooms.create("secret-word").orElseThrow());

        board.handle(connection, new JoinMessage("Mallory", "guess"));

        verify(session, timeout(2000)).close(CloseReason.WRONG_CODE.status());
    }

    @Test
    void tellsSomeoneWhoJoinsAnExpiredRoomThatItIsGone() throws Exception {
        BoardConnection connection = connectTo(rooms.create(null).orElseThrow());
        now = now.plus(IDLE_TIMEOUT);
        rooms.removeIdleRooms();

        board.handle(connection, JOIN);

        verify(session, timeout(2000)).close(CloseReason.ROOM_NOT_FOUND.status());
    }

    private BoardConnection connectTo(Room room) {
        BoardConnection connection = BoardConnection.open(session, room);
        connections.save(connection);
        return connection;
    }
}
