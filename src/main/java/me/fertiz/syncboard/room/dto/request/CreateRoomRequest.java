package me.fertiz.syncboard.room.dto.request;

public record CreateRoomRequest(String code) {

    private static final int MIN_CODE_LENGTH = 4;
    private static final int MAX_CODE_LENGTH = 32;

    public CreateRoomRequest {
        code = code == null || code.isBlank() ? null : code.strip();
    }

    public boolean isValid() {
        return code == null || code.length() >= MIN_CODE_LENGTH && code.length() <= MAX_CODE_LENGTH;
    }
}
