package me.fertiz.syncboard.api;

import me.fertiz.syncboard.room.Room;

public record RoomResponse(String id, boolean locked) {

    static RoomResponse from(Room room) {
        return new RoomResponse(room.id(), room.isLocked());
    }
}
