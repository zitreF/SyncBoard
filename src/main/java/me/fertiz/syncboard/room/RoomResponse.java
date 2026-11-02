package me.fertiz.syncboard.room;

public record RoomResponse(String id, boolean locked) {

    static RoomResponse from(Room room) {
        return new RoomResponse(room.id(), room.isLocked());
    }
}
