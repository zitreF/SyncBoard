package me.fertiz.syncboard.board.dto.response;

import me.fertiz.syncboard.room.Member;

public record MemberResponse(String id, String name, String color) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.id(), member.name(), member.color());
    }
}
