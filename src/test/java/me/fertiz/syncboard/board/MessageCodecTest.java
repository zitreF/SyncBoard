package me.fertiz.syncboard.board;

import me.fertiz.syncboard.board.dto.request.ClearMessage;
import me.fertiz.syncboard.board.dto.request.CursorMessage;
import me.fertiz.syncboard.board.dto.request.EraseMessage;
import me.fertiz.syncboard.board.dto.request.JoinMessage;
import me.fertiz.syncboard.board.dto.response.BoardClearedMessage;
import me.fertiz.syncboard.board.dto.response.CursorMovedMessage;
import me.fertiz.syncboard.board.dto.response.MembersMessage;
import me.fertiz.syncboard.board.dto.response.NoticeMessage;
import me.fertiz.syncboard.board.dto.response.StrokeDrawnMessage;
import me.fertiz.syncboard.board.dto.response.StrokeErasedMessage;
import me.fertiz.syncboard.board.dto.response.WelcomeMessage;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageCodecTest {

    private final MessageCodec codec = new MessageCodec(JsonMapper.builder().build());

    @Test
    void readsEveryMessageTheClientCanSend() {
        assertThat(codec.read("{\"type\":\"join\",\"name\":\"Ola\",\"code\":\"abcd\"}")).contains(new JoinMessage("Ola", "abcd"));
        assertThat(codec.read("{\"type\":\"erase\",\"id\":\"s1\"}")).contains(new EraseMessage("s1"));
        assertThat(codec.read("{\"type\":\"clear\"}")).contains(new ClearMessage());
        assertThat(codec.read("{\"type\":\"cursor\",\"x\":1,\"y\":2}")).contains(new CursorMessage(1, 2));
        assertThat(codec.read("{\"type\":\"draw\",\"id\":\"s1\",\"color\":\"#000000\",\"width\":2,\"points\":[1,2]}")).isPresent();
    }

    @Test
    void writesTheTypeThePageExpects() {
        assertThat(codec.write(new WelcomeMessage("m1", List.of(), List.of()))).contains("\"type\":\"welcome\"");
        assertThat(codec.write(new MembersMessage(List.of()))).contains("\"type\":\"members\"");
        assertThat(codec.write(new StrokeDrawnMessage("s1", "#000000", 2, new int[]{1, 2}))).contains("\"type\":\"draw\"");
        assertThat(codec.write(new StrokeErasedMessage("s1"))).contains("\"type\":\"erase\"");
        assertThat(codec.write(new BoardClearedMessage("m1"))).contains("\"type\":\"clear\"");
        assertThat(codec.write(new CursorMovedMessage("m1", 1, 2))).contains("\"type\":\"cursor\"");
        assertThat(codec.write(new NoticeMessage("full"))).contains("\"type\":\"notice\"");
    }

    @Test
    void readsNothingFromAnUnreadablePayload() {
        assertThat(codec.read("{not json")).isEmpty();
        assertThat(codec.read("{\"type\":\"unknown\"}")).isEmpty();
        assertThat(codec.read("null")).isEmpty();
    }
}
