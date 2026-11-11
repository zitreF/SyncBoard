package me.fertiz.syncboard.board.dto.request;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = JoinMessage.class, name = "join"),
        @JsonSubTypes.Type(value = DrawMessage.class, name = "draw"),
        @JsonSubTypes.Type(value = EraseMessage.class, name = "erase"),
        @JsonSubTypes.Type(value = ClearMessage.class, name = "clear"),
        @JsonSubTypes.Type(value = CursorMessage.class, name = "cursor")
})
public sealed interface ClientMessage permits JoinMessage, DrawMessage, EraseMessage, ClearMessage, CursorMessage {

    default boolean isValid() {
        return true;
    }
}
