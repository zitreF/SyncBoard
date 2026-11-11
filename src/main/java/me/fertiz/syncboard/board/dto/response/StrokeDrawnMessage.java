package me.fertiz.syncboard.board.dto.response;

public record StrokeDrawnMessage(String id, String color, int width, int[] points) implements ServerMessage {
}
