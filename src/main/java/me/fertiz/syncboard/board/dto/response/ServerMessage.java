package me.fertiz.syncboard.board.dto.response;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = WelcomeMessage.class, name = "welcome"),
        @JsonSubTypes.Type(value = MembersMessage.class, name = "members"),
        @JsonSubTypes.Type(value = StrokeDrawnMessage.class, name = "draw"),
        @JsonSubTypes.Type(value = StrokeErasedMessage.class, name = "erase"),
        @JsonSubTypes.Type(value = BoardClearedMessage.class, name = "clear"),
        @JsonSubTypes.Type(value = CursorMovedMessage.class, name = "cursor"),
        @JsonSubTypes.Type(value = NoticeMessage.class, name = "notice")
})
public sealed interface ServerMessage permits WelcomeMessage, MembersMessage, StrokeDrawnMessage, StrokeErasedMessage,
        BoardClearedMessage, CursorMovedMessage, NoticeMessage {
}
