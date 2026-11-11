package me.fertiz.syncboard.room;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DrawingTest {

    private static final String BLACK = "#1e1e1e";

    private final Drawing drawing = new Drawing();

    @Test
    void appendsPointsToAStrokeAndKeepsItsFirstStyle() {
        assertThat(drawing.draw("s1", "#e03131", 4, new int[]{1, 2, 3, 4})).isTrue();
        assertThat(drawing.draw("s1", "#1971c2", 8, new int[]{5, 6})).isTrue();

        Stroke stroke = drawing.strokes().getFirst();
        assertThat(drawing.strokes()).hasSize(1);
        assertThat(stroke.color()).isEqualTo("#e03131");
        assertThat(stroke.width()).isEqualTo(4);
        assertThat(stroke.points()).containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    void keepsStrokesInTheOrderTheyWereStarted() {
        drawing.draw("s1", BLACK, 2, new int[]{1, 1});
        drawing.draw("s2", BLACK, 2, new int[]{2, 2});
        drawing.draw("s1", BLACK, 2, new int[]{3, 3});

        assertThat(drawing.strokes()).extracting(Stroke::id).containsExactly("s1", "s2");
    }

    @Test
    void handsOutStrokesThatDoNotChangeLater() {
        drawing.draw("s1", BLACK, 2, new int[]{1, 1});
        List<Stroke> earlier = drawing.strokes();

        drawing.draw("s1", BLACK, 2, new int[]{2, 2});

        assertThat(earlier.getFirst().points()).containsExactly(1, 1);
        assertThat(drawing.strokes().getFirst().points()).containsExactly(1, 1, 2, 2);
    }

    @Test
    void erasesAndClearsStrokes() {
        drawing.draw("s1", BLACK, 2, new int[]{1, 1});
        drawing.draw("s2", BLACK, 2, new int[]{2, 2});

        assertThat(drawing.erase("s1")).isTrue();
        assertThat(drawing.erase("s1")).isFalse();
        assertThat(drawing.strokes()).extracting(Stroke::id).containsExactly("s2");
        assertThat(drawing.clear()).isTrue();
        assertThat(drawing.clear()).isFalse();
        assertThat(drawing.strokes()).isEmpty();
    }

    @Test
    void refusesMoreStrokesThanTheLimit() {
        for (int i = 0; i < Drawing.MAX_STROKES; i++) {
            assertThat(drawing.draw("s" + i, BLACK, 2, new int[]{i, i})).isTrue();
        }

        assertThat(drawing.draw("one-too-many", BLACK, 2, new int[]{0, 0})).isFalse();
        assertThat(drawing.draw("s0", BLACK, 2, new int[]{1, 1})).isTrue();
    }

    @Test
    void refusesAStrokeLongerThanTheLimit() {
        assertThat(drawing.draw("long", BLACK, 2, new int[Drawing.MAX_COORDINATES_PER_STROKE])).isTrue();

        assertThat(drawing.draw("long", BLACK, 2, new int[]{0, 0})).isFalse();
    }

    @Test
    void refusesMoreCoordinatesThanTheBoardHolds() {
        fillTheBoard();

        assertThat(drawing.draw("one-too-many", BLACK, 2, new int[]{0, 0})).isFalse();
    }

    @Test
    void makesSpaceAgainWhenStrokesAreErasedOrCleared() {
        fillTheBoard();

        drawing.erase("full-0");
        assertThat(drawing.draw("after-erase", BLACK, 2, new int[Drawing.MAX_COORDINATES_PER_STROKE])).isTrue();
        assertThat(drawing.draw("one-too-many", BLACK, 2, new int[]{0, 0})).isFalse();

        drawing.clear();
        assertThat(drawing.draw("after-clear", BLACK, 2, new int[]{0, 0})).isTrue();
    }

    private void fillTheBoard() {
        int strokes = Drawing.MAX_COORDINATES / Drawing.MAX_COORDINATES_PER_STROKE;
        for (int i = 0; i < strokes; i++) {
            assertThat(drawing.draw("full-" + i, BLACK, 2, new int[Drawing.MAX_COORDINATES_PER_STROKE])).isTrue();
        }
    }
}
