package me.fertiz.syncboard.room.dto;

public record CreateRoomRequest(String code) {

    static final int MIN_CODE_LENGTH = 4;
    static final int MAX_CODE_LENGTH = 32;

    public String normalizedCode() {
        return code == null || code.isBlank() ? null : code.strip();
    }

    public static boolean isValidCode(String code) {
        return code == null || code.length() >= MIN_CODE_LENGTH && code.length() <= MAX_CODE_LENGTH;
    }
}
