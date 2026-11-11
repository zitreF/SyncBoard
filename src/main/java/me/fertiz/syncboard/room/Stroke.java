package me.fertiz.syncboard.room;

import java.util.Arrays;

public final class Stroke {

    private final String id;
    private final String color;
    private final int width;
    private final int[] points;

    private Stroke(String id, String color, int width, int[] points) {
        this.id = id;
        this.color = color;
        this.width = width;
        this.points = points;
    }

    public static Stroke start(String id, String color, int width, int[] points) {
        return new Stroke(id, color, width, points.clone());
    }

    public Stroke extendedBy(int[] added) {
        int[] extended = Arrays.copyOf(points, points.length + added.length);
        System.arraycopy(added, 0, extended, points.length, added.length);
        return new Stroke(id, color, width, extended);
    }

    public String id() {
        return id;
    }

    public String color() {
        return color;
    }

    public int width() {
        return width;
    }

    public int coordinateCount() {
        return points.length;
    }

    public int[] points() {
        return points.clone();
    }
}
