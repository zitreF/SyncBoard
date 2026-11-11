package me.fertiz.syncboard.room;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class RoomTest {

    private static final Instant NOW = Instant.parse("2026-11-02T12:00:00Z");
    private static final Duration TIMEOUT = Duration.ofMinutes(30);
    private static final Instant IDLE_LONG_ENOUGH = NOW.plus(TIMEOUT);

    private final Room room = new Room("abcd2345", null, NOW);

    @Test
    void runsItsTasksOneAfterAnotherOnOneThread() {
        List<Integer> order = new ArrayList<>();
        Set<Thread> threads = ConcurrentHashMap.newKeySet();

        CompletableFuture.allOf(IntStream.range(0, 200)
                .mapToObj(i -> room.execute(state -> {
                    order.add(i);
                    threads.add(Thread.currentThread());
                }))
                .toArray(CompletableFuture[]::new)).join();

        assertThat(order).containsExactlyElementsOf(IntStream.range(0, 200).boxed().toList());
        assertThat(threads).hasSize(1).doesNotContain(Thread.currentThread());
    }

    @Test
    void keepsEveryPointWhenManyPeopleDrawAtOnce() {
        List<CompletableFuture<Void>> draws = new CopyOnWriteArrayList<>();
        try (ExecutorService people = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int person = 0; person < 20; person++) {
                people.submit(() -> {
                    for (int i = 0; i < 100; i++) {
                        int[] point = {i, i};
                        draws.add(room.execute(state -> state.drawing().draw("shared", "#1e1e1e", 2, point)));
                    }
                });
            }
        }

        CompletableFuture.allOf(draws.toArray(CompletableFuture[]::new)).join();

        assertThat(strokes()).singleElement().satisfies(stroke -> assertThat(stroke.coordinateCount()).isEqualTo(4000));
    }

    @Test
    void closesOnlyAfterBeingIdleForTheWholeTimeout() {
        assertThat(room.closeIfIdle(IDLE_LONG_ENOUGH.minusSeconds(1), TIMEOUT).join()).isFalse();
        assertThat(room.closeIfIdle(IDLE_LONG_ENOUGH, TIMEOUT).join()).isTrue();
    }

    @Test
    void refusesTasksOnceClosed() {
        room.closeIfIdle(IDLE_LONG_ENOUGH, TIMEOUT).join();

        assertThat(room.execute(state -> state.drawing().clear())).isCompletedExceptionally();
    }

    @Test
    void refusesToCloseTwice() {
        room.closeIfIdle(IDLE_LONG_ENOUGH, TIMEOUT).join();

        assertThat(room.closeIfIdle(IDLE_LONG_ENOUGH, TIMEOUT)).isCompletedExceptionally();
    }

    @Test
    void tasksQueuedBeforeClosingSeeTheRoomAsClosed() {
        CountDownLatch release = new CountDownLatch(1);
        room.execute(state -> await(release));
        room.closeIfIdle(IDLE_LONG_ENOUGH, TIMEOUT);
        AtomicBoolean seenClosed = new AtomicBoolean();
        CompletableFuture<Void> queued = room.execute(state -> seenClosed.set(room.isClosed()));

        release.countDown();
        queued.join();

        assertThat(seenClosed).isTrue();
    }

    @Test
    void checksTheJoinCode() {
        Room locked = new Room("abcd2345", "secret-word", NOW);

        assertThat(room.isLocked()).isFalse();
        assertThat(room.accepts(null)).isTrue();
        assertThat(locked.isLocked()).isTrue();
        assertThat(locked.accepts("secret-word")).isTrue();
        assertThat(locked.accepts("Secret-word")).isFalse();
        assertThat(locked.accepts("")).isFalse();
        assertThat(locked.accepts(null)).isFalse();
    }

    private List<Stroke> strokes() {
        AtomicReference<List<Stroke>> strokes = new AtomicReference<>();
        room.execute(state -> strokes.set(state.drawing().strokes())).join();
        return strokes.get();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
