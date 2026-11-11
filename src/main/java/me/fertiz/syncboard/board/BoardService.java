package me.fertiz.syncboard.board;

import me.fertiz.syncboard.board.dto.request.ClearMessage;
import me.fertiz.syncboard.board.dto.request.ClientMessage;
import me.fertiz.syncboard.board.dto.request.CursorMessage;
import me.fertiz.syncboard.board.dto.request.DrawMessage;
import me.fertiz.syncboard.board.dto.request.EraseMessage;
import me.fertiz.syncboard.board.dto.request.JoinMessage;
import me.fertiz.syncboard.board.dto.response.BoardClearedMessage;
import me.fertiz.syncboard.board.dto.response.CursorMovedMessage;
import me.fertiz.syncboard.board.dto.response.MembersMessage;
import me.fertiz.syncboard.board.dto.response.NoticeMessage;
import me.fertiz.syncboard.board.dto.response.StrokeDrawnMessage;
import me.fertiz.syncboard.board.dto.response.StrokeErasedMessage;
import me.fertiz.syncboard.board.dto.response.WelcomeMessage;
import me.fertiz.syncboard.room.Member;
import me.fertiz.syncboard.room.Room;
import me.fertiz.syncboard.room.RoomMembers;
import me.fertiz.syncboard.room.RoomState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.InstantSource;
import java.util.Optional;
import java.util.function.Consumer;

@Service
public class BoardService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BoardService.class);
    private static final String BOARD_FULL = "The board is full, clear something to keep drawing";

    private final BoardBroadcaster broadcaster;
    private final InstantSource clock;

    public BoardService(BoardBroadcaster broadcaster, InstantSource clock) {
        this.broadcaster = broadcaster;
        this.clock = clock;
    }

    public void handle(BoardConnection sender, ClientMessage message) {
        inRoom(sender, state -> {
            Optional<Member> member = state.members().find(sender.id());
            if (member.isPresent()) {
                handleFromMember(sender, state, member.get(), message);
            } else {
                handleBeforeJoining(sender, state, message);
            }
        });
    }

    public void leave(BoardConnection connection) {
        inRoom(connection, state -> {
            RoomMembers members = state.members();
            Optional<Member> left = members.leave(connection.id(), clock.instant());
            if (left.isPresent()) {
                broadcaster.sendToEveryone(members, MembersMessage.of(members.all()));
            }
        });
    }

    private void inRoom(BoardConnection connection, Consumer<RoomState> task) {
        connection.room().execute(task).exceptionally(error -> {
            closeAfterFailure(connection, error);
            return null;
        });
    }

    private void closeAfterFailure(BoardConnection connection, Throwable error) {
        Room room = connection.room();
        if (room.isClosed()) {
            connection.close(CloseReason.ROOM_NOT_FOUND);
        } else {
            LOGGER.warn("Room {} could not handle a message from {}", room.id(), connection.id(), error);
            connection.close(CloseReason.SERVER_ERROR);
        }
    }

    private void handleBeforeJoining(BoardConnection sender, RoomState state, ClientMessage message) {
        if (message instanceof JoinMessage join) {
            join(sender, state, join);
        } else {
            sender.close(CloseReason.BAD_MESSAGE);
        }
    }

    private void handleFromMember(BoardConnection sender, RoomState state, Member member, ClientMessage message) {
        switch (message) {
            case DrawMessage draw -> draw(sender, state, draw);
            case EraseMessage erase -> erase(sender, state, erase);
            case ClearMessage _ -> clear(sender, state, member);
            case CursorMessage cursor -> moveCursor(sender, state, member, cursor);
            case JoinMessage _ -> sender.close(CloseReason.BAD_MESSAGE);
        }
    }

    private void join(BoardConnection sender, RoomState state, JoinMessage join) {
        Room room = sender.room();
        RoomMembers members = state.members();
        if (room.isClosed()) {
            sender.close(CloseReason.ROOM_NOT_FOUND);
        } else if (!room.accepts(join.code())) {
            sender.close(CloseReason.WRONG_CODE);
        } else if (members.isFull()) {
            sender.close(CloseReason.ROOM_FULL);
        } else {
            Member member = members.join(sender.id(), join.name());
            broadcaster.send(sender, WelcomeMessage.of(member, members.all(), state.drawing().strokes()));
            broadcaster.sendToOthers(members, sender, MembersMessage.of(members.all()));
        }
    }

    private void draw(BoardConnection sender, RoomState state, DrawMessage draw) {
        if (state.drawing().draw(draw.id(), draw.color(), draw.width(), draw.points())) {
            StrokeDrawnMessage drawn = new StrokeDrawnMessage(draw.id(), draw.color(), draw.width(), draw.points());
            broadcaster.sendToOthers(state.members(), sender, drawn);
        } else {
            refuseStroke(sender, state, draw.id());
        }
    }

    private void refuseStroke(BoardConnection sender, RoomState state, String strokeId) {
        state.drawing().erase(strokeId);
        broadcaster.sendToEveryone(state.members(), new StrokeErasedMessage(strokeId));
        broadcaster.send(sender, new NoticeMessage(BOARD_FULL));
    }

    private void erase(BoardConnection sender, RoomState state, EraseMessage erase) {
        if (state.drawing().erase(erase.id())) {
            broadcaster.sendToOthers(state.members(), sender, new StrokeErasedMessage(erase.id()));
        }
    }

    private void clear(BoardConnection sender, RoomState state, Member member) {
        if (state.drawing().clear()) {
            broadcaster.sendToOthers(state.members(), sender, new BoardClearedMessage(member.id()));
        }
    }

    private void moveCursor(BoardConnection sender, RoomState state, Member member, CursorMessage cursor) {
        CursorMovedMessage moved = new CursorMovedMessage(member.id(), cursor.x(), cursor.y());
        broadcaster.sendToOthers(state.members(), sender, moved);
    }
}
