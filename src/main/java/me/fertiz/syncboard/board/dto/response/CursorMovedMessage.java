package me.fertiz.syncboard.board.dto.response;

public record CursorMovedMessage(String member, Integer x, Integer y) implements ServerMessage {
}
