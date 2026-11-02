package me.fertiz.syncboard.socket;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClientMessageTest {

    private static ClientMessage.Draw draw(String id, String color, Integer width, int... points) {
        return new ClientMessage.Draw(id, color, width, points);
    }

    @Test
    void acceptsAWellFormedDraw() {
        assertThat(draw("k3x9-a_b", "#1E1e1e", 4, 10, 20, -30, 40).isValid()).isTrue();
        assertThat(draw("s", "#000000", 1, 0, 0).isValid()).isTrue();
        assertThat(draw("s", "#000000", 50, new int[ClientMessage.Draw.MAX_COORDINATES]).isValid()).isTrue();
    }

    @Test
    void rejectsDrawsWithBadFields() {
        assertThat(draw(null, "#000000", 4, 1, 1).isValid()).isFalse();
        assertThat(draw("", "#000000", 4, 1, 1).isValid()).isFalse();
        assertThat(draw("a".repeat(41), "#000000", 4, 1, 1).isValid()).isFalse();
        assertThat(draw("s s", "#000000", 4, 1, 1).isValid()).isFalse();
        assertThat(draw("s", "red", 4, 1, 1).isValid()).isFalse();
        assertThat(draw("s", "#00000", 4, 1, 1).isValid()).isFalse();
        assertThat(draw("s", "#000000;x", 4, 1, 1).isValid()).isFalse();
        assertThat(draw("s", "#000000", null, 1, 1).isValid()).isFalse();
        assertThat(draw("s", "#000000", 0, 1, 1).isValid()).isFalse();
        assertThat(draw("s", "#000000", 51, 1, 1).isValid()).isFalse();
    }

    @Test
    void rejectsDrawsWithBadPoints() {
        assertThat(new ClientMessage.Draw("s", "#000000", 4, null).isValid()).isFalse();
        assertThat(draw("s", "#000000", 4).isValid()).isFalse();
        assertThat(draw("s", "#000000", 4, 1, 2, 3).isValid()).isFalse();
        assertThat(draw("s", "#000000", 4, new int[ClientMessage.Draw.MAX_COORDINATES + 2]).isValid()).isFalse();
        assertThat(draw("s", "#000000", 4, 1, ClientMessage.MAX_COORDINATE + 1).isValid()).isFalse();
        assertThat(draw("s", "#000000", 4, -ClientMessage.MAX_COORDINATE - 1, 1).isValid()).isFalse();
    }

    @Test
    void acceptsCursorsOnTheBoardOrHidden() {
        assertThat(new ClientMessage.Cursor(10, -20).isValid()).isTrue();
        assertThat(new ClientMessage.Cursor(null, null).isValid()).isTrue();
        assertThat(new ClientMessage.Cursor(null, null).isHidden()).isTrue();
        assertThat(new ClientMessage.Cursor(10, null).isValid()).isFalse();
        assertThat(new ClientMessage.Cursor(ClientMessage.MAX_COORDINATE + 1, 0).isValid()).isFalse();
    }
}
