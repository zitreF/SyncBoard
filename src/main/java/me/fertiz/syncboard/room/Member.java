package me.fertiz.syncboard.room;

import java.util.regex.Pattern;

public record Member(String id, String name, String color) {

    public static final int MAX_NAME_LENGTH = 30;
    public static final String DEFAULT_NAME = "Guest";

    private static final Pattern INVISIBLE = Pattern.compile("[\\p{Cc}\\p{Cf}&&[^\\s]]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\p{Z}\\s]+");

    public static String cleanName(String name) {
        if (name == null) {
            return DEFAULT_NAME;
        }
        String visible = INVISIBLE.matcher(name).replaceAll("");
        String spaced = WHITESPACE.matcher(visible).replaceAll(" ").strip();
        String clean = shortened(spaced);
        return clean.isEmpty() ? DEFAULT_NAME : clean;
    }

    private static String shortened(String name) {
        if (name.codePointCount(0, name.length()) <= MAX_NAME_LENGTH) {
            return name;
        }
        return name.substring(0, name.offsetByCodePoints(0, MAX_NAME_LENGTH)).strip();
    }
}
