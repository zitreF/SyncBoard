package me.fertiz.syncboard.room.dto.response;

import me.fertiz.syncboard.room.Room;

public record RoomResponse(String id, boolean locked) {

    public static RoomResponse from(Room room) {
        return new RoomResponse(room.id(), room.isLocked());
    }
}
