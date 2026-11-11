package me.fertiz.syncboard.room;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StrokeTest {

    @Test
    void growsWithoutChangingTheStrokeItCameFrom() {
        Stroke start = Stroke.start("s1", "#e03131", 4, new int[]{1, 2});

        Stroke longer = start.extendedBy(new int[]{3, 4, 5, 6});

        assertThat(start.points()).containsExactly(1, 2);
        assertThat(longer.points()).containsExactly(1, 2, 3, 4, 5, 6);
        assertThat(longer.coordinateCount()).isEqualTo(6);
        assertThat(longer.id()).isEqualTo("s1");
        assertThat(longer.color()).isEqualTo("#e03131");
        assertThat(longer.width()).isEqualTo(4);
    }

    @Test
    void cannotBeChangedThroughTheArraysItTakesOrGives() {
        int[] given = {1, 2};
        Stroke stroke = Stroke.start("s1", "#e03131", 4, given);

        given[0] = 99;
        stroke.points()[1] = 99;

        assertThat(stroke.points()).containsExactly(1, 2);
    }
}
