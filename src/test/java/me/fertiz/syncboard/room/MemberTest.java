package me.fertiz.syncboard.room;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    @Test
    void cleansNames() {
        assertThat(Member.cleanName("  Bob\tthe​  builder\n")).isEqualTo("Bob the builder");
        assertThat(Member.cleanName("‮​")).isEqualTo("Guest");
        assertThat(Member.cleanName(null)).isEqualTo("Guest");
        assertThat(Member.cleanName("x".repeat(50))).hasSize(Member.MAX_NAME_LENGTH);
    }
}
