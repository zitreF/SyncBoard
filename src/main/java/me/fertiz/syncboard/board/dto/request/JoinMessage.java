package me.fertiz.syncboard.board.dto.request;

public record JoinMessage(String name, String code) implements ClientMessage {
}
