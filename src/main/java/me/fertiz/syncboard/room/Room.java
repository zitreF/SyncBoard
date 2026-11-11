package me.fertiz.syncboard.room;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Room {

    private final String id;
    private final byte[] code;
    private final RoomState state;
    private final ExecutorService tasks;

    public Room(String id, String code, Instant now) {
        this.id = id;
        this.code = code == null ? null : code.getBytes(StandardCharsets.UTF_8);
        this.state = new RoomState(new RoomMembers(now), new Drawing());
        this.tasks = Executors.newSingleThreadExecutor(Thread.ofVirtual().name("room-" + id).factory());
    }

    public String id() {
        return id;
    }

    public boolean isLocked() {
        return code != null;
    }

    public boolean accepts(String attempt) {
        if (!isLocked()) {
            return true;
        }
        return attempt != null && MessageDigest.isEqual(code, attempt.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isClosed() {
        return tasks.isShutdown();
    }

    public CompletableFuture<Void> execute(Consumer<RoomState> task) {
        return submit(() -> {
            task.accept(state);
            return null;
        });
    }

    public CompletableFuture<Boolean> closeIfIdle(Instant now, Duration timeout) {
        return submit(() -> {
            boolean idle = state.members().hasBeenEmptyFor(timeout, now);
            if (idle) {
                tasks.shutdown();
            }
            return idle;
        });
    }

    private <T> CompletableFuture<T> submit(Supplier<T> task) {
        try {
            return CompletableFuture.supplyAsync(task, tasks);
        } catch (RejectedExecutionException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
