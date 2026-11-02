package me.fertiz.syncboard.room;

import java.util.regex.Pattern;

public record Member(String id, String name, String color) {

    static final int MAX_NAME_LENGTH = 30;

    private static final Pattern WHITESPACE = Pattern.compile("[\\p{Z}\\s]+");
    private static final Pattern INVISIBLE = Pattern.compile("[\\p{Cc}\\p{Cf}]");

    static String cleanName(String name) {
        if (name == null) {
            return "Guest";
        }
        String spaced = WHITESPACE.matcher(name).replaceAll(" ");
        String clean = INVISIBLE.matcher(spaced).replaceAll("").replaceAll(" {2,}", " ").strip();
        if (clean.codePointCount(0, clean.length()) > MAX_NAME_LENGTH) {
            clean = clean.substring(0, clean.offsetByCodePoints(0, MAX_NAME_LENGTH)).strip();
        }
        return clean.isEmpty() ? "Guest" : clean;
    }
}
