package me.fertiz.syncboard.room;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class RoomMembers {

    public static final int MAX_MEMBERS = 20;

    public static final List<String> COLORS = List.of(
            "#e03131", "#1971c2", "#2f9e44", "#f08c00", "#9c36b5", "#0c8599", "#e8590c", "#5f3dc4");

    private final Map<String, Member> members = new LinkedHashMap<>();
    private int joinCount;
    private Instant emptySince;

    public RoomMembers(Instant now) {
        this.emptySince = now;
    }

    public boolean isFull() {
        return members.size() >= MAX_MEMBERS;
    }

    public Member join(String connectionId, String name) {
        joinCount++;
        Member member = new Member("m" + joinCount, Member.cleanName(name), freeColor());
        members.put(connectionId, member);
        return member;
    }

    public Optional<Member> leave(String connectionId, Instant now) {
        Member left = members.remove(connectionId);
        if (left != null && members.isEmpty()) {
            emptySince = now;
        }
        return Optional.ofNullable(left);
    }

    public Optional<Member> find(String connectionId) {
        return Optional.ofNullable(members.get(connectionId));
    }

    public List<Member> all() {
        return List.copyOf(members.values());
    }

    public Collection<String> connectionIds() {
        return Collections.unmodifiableSet(members.keySet());
    }

    public boolean hasBeenEmptyFor(Duration timeout, Instant now) {
        return members.isEmpty() && !emptySince.plus(timeout).isAfter(now);
    }

    private String freeColor() {
        List<String> taken = members.values().stream().map(Member::color).toList();
        return COLORS.stream()
                .filter(color -> !taken.contains(color))
                .findFirst()
                .orElse(COLORS.get(joinCount % COLORS.size()));
    }
}
