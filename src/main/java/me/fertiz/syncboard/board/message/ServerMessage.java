package me.fertiz.syncboard.board.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import me.fertiz.syncboard.room.Member;
import me.fertiz.syncboard.room.Stroke;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ServerMessage.Welcome.class, name = "welcome"),
        @JsonSubTypes.Type(value = ServerMessage.Members.class, name = "members"),
        @JsonSubTypes.Type(value = ServerMessage.Draw.class, name = "draw"),
        @JsonSubTypes.Type(value = ServerMessage.Erase.class, name = "erase"),
        @JsonSubTypes.Type(value = ServerMessage.Clear.class, name = "clear"),
        @JsonSubTypes.Type(value = ServerMessage.Cursor.class, name = "cursor"),
        @JsonSubTypes.Type(value = ServerMessage.Notice.class, name = "notice")
})
public sealed interface ServerMessage {

    record Welcome(String you, List<Member> members, List<Stroke> strokes) implements ServerMessage {}

    record Members(List<Member> members) implements ServerMessage {}

    record Draw(String id, String color, int width, int[] points) implements ServerMessage {}

    record Erase(String id) implements ServerMessage {}

    record Clear(String by) implements ServerMessage {}

    record Cursor(String member, Integer x, Integer y) implements ServerMessage {}

    record Notice(String message) implements ServerMessage {}
}
