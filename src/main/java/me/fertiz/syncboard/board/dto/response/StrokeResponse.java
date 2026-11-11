package me.fertiz.syncboard.board.dto.response;

import me.fertiz.syncboard.room.Stroke;

public record StrokeResponse(String id, String color, int width, int[] points) {

    public static StrokeResponse from(Stroke stroke) {
        return new StrokeResponse(stroke.id(), stroke.color(), stroke.width(), stroke.points());
    }
}
