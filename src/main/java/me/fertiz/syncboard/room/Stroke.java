package me.fertiz.syncboard.room;

import java.util.ArrayList;
import java.util.List;

public record Stroke(String id, String color, int width, List<Integer> points) {

    static Stroke start(String id, String color, int width) {
        return new Stroke(id, color, width, new ArrayList<>());
    }

    void append(int[] coordinates) {
        for (int coordinate : coordinates) {
            points.add(coordinate);
        }
    }
}
