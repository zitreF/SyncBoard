package me.fertiz.syncboard.room;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Room {

    static final int MAX_MEMBERS = 20;
    static final int MAX_STROKES = 10_000;
    static final int MAX_STROKE_COORDINATES = 20_000;

    static final List<String> COLORS = List.of(
            "#e03131", "#1971c2", "#2f9e44", "#f08c00", "#9c36b5", "#0c8599", "#e8590c", "#5f3dc4");

    private final String id;
    private final byte[] code;
    private final Map<String, Stroke> strokes = new LinkedHashMap<>();
    private final Map<String, Member> members = new LinkedHashMap<>();
    private int joined;
    private Instant lastActive;
    private boolean closed;

    Room(String id, String code, Instant now) {
        this.id = id;
        this.code = code == null ? null : code.getBytes(StandardCharsets.UTF_8);
        this.lastActive = now;
    }

    public String id() {
        return id;
    }

    public boolean isLocked() {
        return code != null;
    }

    public boolean accepts(String attempt) {
        return code == null || attempt != null && MessageDigest.isEqual(code, attempt.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isFull() {
        return members.size() >= MAX_MEMBERS;
    }

    public Member join(String sessionId, String name) {
        joined++;
        Member member = new Member("m" + joined, Member.cleanName(name), freeColor());
        members.put(sessionId, member);
        return member;
    }

    public Member leave(String sessionId, Instant now) {
        lastActive = now;
        return members.remove(sessionId);
    }

    public Member member(String sessionId) {
        return members.get(sessionId);
    }

    public List<Member> members() {
        return List.copyOf(members.values());
    }

    public Set<String> sessionIds() {
        return Set.copyOf(members.keySet());
    }

    public boolean draw(String strokeId, String color, int width, int[] coordinates) {
        Stroke stroke = strokes.get(strokeId);
        if (stroke == null) {
            if (strokes.size() >= MAX_STROKES) {
                return false;
            }
            stroke = Stroke.start(strokeId, color, width);
            strokes.put(strokeId, stroke);
        }
        if (stroke.points().size() + coordinates.length > MAX_STROKE_COORDINATES) {
            return false;
        }
        stroke.append(coordinates);
        return true;
    }

    public boolean erase(String strokeId) {
        return strokes.remove(strokeId) != null;
    }

    public boolean clear() {
        boolean hadStrokes = !strokes.isEmpty();
        strokes.clear();
        return hadStrokes;
    }

    public List<Stroke> strokes() {
        return strokes.values().stream().map(Stroke::copy).toList();
    }

    boolean isIdle(Instant now, Duration timeout) {
        return members.isEmpty() && !lastActive.plus(timeout).isAfter(now);
    }

    void close() {
        closed = true;
    }

    private String freeColor() {
        List<String> taken = members.values().stream().map(Member::color).toList();
        return COLORS.stream()
                .filter(color -> !taken.contains(color))
                .findFirst()
                .orElse(COLORS.get(joined % COLORS.size()));
    }
}
