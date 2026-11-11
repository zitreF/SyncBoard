package me.fertiz.syncboard.board.dto.request;

public final class Coordinates {

    public static final int MAX = 100_000;

    private Coordinates() {
    }

    public static boolean isAllowed(int coordinate) {
        return coordinate >= -MAX && coordinate <= MAX;
    }

    public static boolean areAllowed(int[] coordinates) {
        for (int coordinate : coordinates) {
            if (!isAllowed(coordinate)) {
                return false;
            }
        }
        return true;
    }
}
