package me.fertiz.syncboard.board.dto.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DrawMessageTest {

    private static DrawMessage draw(String id, String color, Integer width, int... points) {
        return new DrawMessage(id, color, width, points);
    }

    @Test
    void acceptsAWellFormedDraw() {
        assertThat(draw("k3x9-a_b", "#1E1e1e", 4, 10, 20, -30, 40).isValid()).isTrue();
        assertThat(draw("s", "#000000", 1, 0, 0).isValid()).isTrue();
        assertThat(draw("s", "#000000", 50, new int[DrawMessage.MAX_COORDINATES]).isValid()).isTrue();
    }

    @Test
    void rejectsABadId() {
        assertThat(draw(null, "#000000", 4, 1, 1).isValid()).isFalse();
        assertThat(draw("", "#000000", 4, 1, 1).isValid()).isFalse();
        assertThat(draw("a".repeat(41), "#000000", 4, 1, 1).isValid()).isFalse();
        assertThat(draw("s s", "#000000", 4, 1, 1).isValid()).isFalse();
    }

    @Test
    void rejectsABadColor() {
        assertThat(draw("s", null, 4, 1, 1).isValid()).isFalse();
        assertThat(draw("s", "red", 4, 1, 1).isValid()).isFalse();
        assertThat(draw("s", "#00000", 4, 1, 1).isValid()).isFalse();
        assertThat(draw("s", "#000000;x", 4, 1, 1).isValid()).isFalse();
    }

    @Test
    void rejectsABadWidth() {
        assertThat(draw("s", "#000000", null, 1, 1).isValid()).isFalse();
        assertThat(draw("s", "#000000", 0, 1, 1).isValid()).isFalse();
        assertThat(draw("s", "#000000", DrawMessage.MAX_WIDTH + 1, 1, 1).isValid()).isFalse();
    }

    @Test
    void rejectsBadPoints() {
        assertThat(new DrawMessage("s", "#000000", 4, null).isValid()).isFalse();
        assertThat(draw("s", "#000000", 4).isValid()).isFalse();
        assertThat(draw("s", "#000000", 4, 1, 2, 3).isValid()).isFalse();
        assertThat(draw("s", "#000000", 4, new int[DrawMessage.MAX_COORDINATES + 2]).isValid()).isFalse();
    }

    @Test
    void rejectsCoordinatesOffTheBoard() {
        assertThat(draw("s", "#000000", 4, 1, Coordinates.MAX).isValid()).isTrue();
        assertThat(draw("s", "#000000", 4, 1, Coordinates.MAX + 1).isValid()).isFalse();
        assertThat(draw("s", "#000000", 4, -Coordinates.MAX - 1, 1).isValid()).isFalse();
        assertThat(draw("s", "#000000", 4, Integer.MIN_VALUE, 1).isValid()).isFalse();
    }
}
