package me.fertiz.syncboard.board.dto.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CursorMessageTest {

    @Test
    void acceptsACursorOnTheBoard() {
        assertThat(new CursorMessage(10, -20).isValid()).isTrue();
    }

    @Test
    void acceptsAHiddenCursor() {
        assertThat(new CursorMessage(null, null).isValid()).isTrue();
    }

    @Test
    void rejectsHalfACursor() {
        assertThat(new CursorMessage(10, null).isValid()).isFalse();
        assertThat(new CursorMessage(null, 10).isValid()).isFalse();
    }

    @Test
    void rejectsACursorOffTheBoard() {
        assertThat(new CursorMessage(Coordinates.MAX + 1, 0).isValid()).isFalse();
        assertThat(new CursorMessage(0, Integer.MIN_VALUE).isValid()).isFalse();
    }
}
