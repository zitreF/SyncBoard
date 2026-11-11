package me.fertiz.syncboard.board.dto.response;

import me.fertiz.syncboard.room.Member;
import me.fertiz.syncboard.room.Stroke;

import java.util.List;

public record WelcomeMessage(String you, List<MemberResponse> members, List<StrokeResponse> strokes)
        implements ServerMessage {

    public static WelcomeMessage of(Member you, List<Member> members, List<Stroke> strokes) {
        return new WelcomeMessage(
                you.id(),
                members.stream().map(MemberResponse::from).toList(),
                strokes.stream().map(StrokeResponse::from).toList());
    }
}
