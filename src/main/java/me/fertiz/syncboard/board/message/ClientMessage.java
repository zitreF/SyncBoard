package me.fertiz.syncboard.board.message;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Arrays;
import java.util.regex.Pattern;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ClientMessage.Join.class, name = "join"),
        @JsonSubTypes.Type(value = ClientMessage.Draw.class, name = "draw"),
        @JsonSubTypes.Type(value = ClientMessage.Erase.class, name = "erase"),
        @JsonSubTypes.Type(value = ClientMessage.Clear.class, name = "clear"),
        @JsonSubTypes.Type(value = ClientMessage.Cursor.class, name = "cursor")
})
public sealed interface ClientMessage {

    int MAX_COORDINATE = 100_000;

    record Join(String name, String code) implements ClientMessage {}

    record Draw(String id, String color, Integer width, int[] points) implements ClientMessage {

        static final int MAX_WIDTH = 50;
        static final int MAX_COORDINATES = 2_000;

        private static final Pattern STROKE_ID = Pattern.compile("[A-Za-z0-9_-]{1,40}");
        private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

        public boolean isValid() {
            return id != null && STROKE_ID.matcher(id).matches()
                    && color != null && COLOR.matcher(color).matches()
                    && width != null && width >= 1 && width <= MAX_WIDTH
                    && points != null && points.length >= 2 && points.length % 2 == 0 && points.length <= MAX_COORDINATES
                    && Arrays.stream(points).allMatch(coordinate -> Math.abs(coordinate) <= MAX_COORDINATE);
        }
    }

    record Erase(String id) implements ClientMessage {}

    record Clear() implements ClientMessage {}

    record Cursor(Integer x, Integer y) implements ClientMessage {

        public boolean isHidden() {
            return x == null && y == null;
        }

        public boolean isValid() {
            return isHidden() || x != null && y != null && Math.abs(x) <= MAX_COORDINATE && Math.abs(y) <= MAX_COORDINATE;
        }
    }
}
