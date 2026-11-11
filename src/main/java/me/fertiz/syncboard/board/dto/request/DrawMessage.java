package me.fertiz.syncboard.board.dto.request;

import java.util.regex.Pattern;

public record DrawMessage(String id, String color, Integer width, int[] points) implements ClientMessage {

    public static final int MAX_WIDTH = 50;
    public static final int MAX_COORDINATES = 2_000;

    private static final Pattern STROKE_ID = Pattern.compile("[A-Za-z0-9_-]{1,40}");
    private static final Pattern COLOR = Pattern.compile("#[0-9a-fA-F]{6}");

    @Override
    public boolean isValid() {
        return matches(STROKE_ID, id) && matches(COLOR, color) && hasAllowedWidth() && hasAllowedPoints();
    }

    private boolean hasAllowedWidth() {
        return width != null && width >= 1 && width <= MAX_WIDTH;
    }

    private boolean hasAllowedPoints() {
        return points != null
                && points.length >= 2
                && points.length % 2 == 0
                && points.length <= MAX_COORDINATES
                && Coordinates.areAllowed(points);
    }

    private static boolean matches(Pattern pattern, String value) {
        return value != null && pattern.matcher(value).matches();
    }
}
