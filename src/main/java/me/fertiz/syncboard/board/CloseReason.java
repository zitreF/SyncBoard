package me.fertiz.syncboard.board;

import org.springframework.web.socket.CloseStatus;

public enum CloseReason {

    BAD_MESSAGE(4400, "Unexpected message"),
    WRONG_CODE(4403, "Wrong join code"),
    ROOM_NOT_FOUND(4404, "This board does not exist"),
    ROOM_FULL(4409, "This board is full"),
    SERVER_ERROR(1011, "Something went wrong"),
    TOO_SLOW(1013, "Too far behind");

    private final CloseStatus status;

    private CloseReason(int code, String description) {
        this.status = new CloseStatus(code, description);
    }

    public CloseStatus status() {
        return status;
    }
}
