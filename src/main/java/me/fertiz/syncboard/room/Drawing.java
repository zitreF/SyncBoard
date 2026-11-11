package me.fertiz.syncboard.room;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Drawing {

    public static final int MAX_STROKES = 10_000;
    public static final int MAX_COORDINATES = 200_000;
    public static final int MAX_COORDINATES_PER_STROKE = 5_000;

    private final Map<String, Stroke> strokes = new LinkedHashMap<>();
    private int coordinateCount;

    public boolean draw(String strokeId, String color, int width, int[] coordinates) {
        Stroke existing = strokes.get(strokeId);
        if (!hasSpaceFor(existing, coordinates.length)) {
            return false;
        }
        Stroke extended = existing == null
                ? Stroke.start(strokeId, color, width, coordinates)
                : existing.extendedBy(coordinates);
        strokes.put(strokeId, extended);
        coordinateCount += coordinates.length;
        return true;
    }

    public boolean erase(String strokeId) {
        Stroke erased = strokes.remove(strokeId);
        if (erased == null) {
            return false;
        }
        coordinateCount -= erased.coordinateCount();
        return true;
    }

    public boolean clear() {
        boolean hadStrokes = !strokes.isEmpty();
        strokes.clear();
        coordinateCount = 0;
        return hadStrokes;
    }

    public List<Stroke> strokes() {
        return List.copyOf(strokes.values());
    }

    private boolean hasSpaceFor(Stroke existing, int addedCoordinates) {
        if (existing == null && strokes.size() >= MAX_STROKES) {
            return false;
        }
        int strokeCoordinates = existing == null ? 0 : existing.coordinateCount();
        return strokeCoordinates + addedCoordinates <= MAX_COORDINATES_PER_STROKE
                && coordinateCount + addedCoordinates <= MAX_COORDINATES;
    }
}
