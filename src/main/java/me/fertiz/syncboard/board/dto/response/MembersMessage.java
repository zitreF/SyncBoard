package me.fertiz.syncboard.board.dto.response;

import me.fertiz.syncboard.room.Member;

import java.util.List;

public record MembersMessage(List<MemberResponse> members) implements ServerMessage {

    public static MembersMessage of(List<Member> members) {
        return new MembersMessage(members.stream().map(MemberResponse::from).toList());
    }
}
