package me.fertiz.syncboard.room;

public record CreateRoomRequest(String code) {

    static final int MIN_CODE_LENGTH = 4;
    static final int MAX_CODE_LENGTH = 32;

    String normalizedCode() {
        return code == null || code.isBlank() ? null : code.strip();
    }

    static boolean isValidCode(String code) {
        return code == null || code.length() >= MIN_CODE_LENGTH && code.length() <= MAX_CODE_LENGTH;
    }
}
