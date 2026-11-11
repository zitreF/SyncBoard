package me.fertiz.syncboard.board;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OutboxTest {

    private final WebSocketSession session = mock(WebSocketSession.class);

    @BeforeEach
    void setUp() {
        when(session.getId()).thenReturn("session-1");
    }

    @Test
    void sendsInOrderAndClosesAfterTheLastMessage() throws Exception {
        Outbox outbox = new Outbox(session, 10);

        outbox.send("a");
        outbox.send("b");
        outbox.close(CloseReason.BAD_MESSAGE);

        verify(session, timeout(2000)).close(CloseReason.BAD_MESSAGE.status());
        InOrder order = inOrder(session);
        order.verify(session).sendMessage(new TextMessage("a"));
        order.verify(session).sendMessage(new TextMessage("b"));
        order.verify(session).close(CloseReason.BAD_MESSAGE.status());
        outbox.stop();
    }

    @Test
    void closesAStalledClientInsteadOfMakingTheRoomWait() throws Exception {
        CountDownLatch stalled = new CountDownLatch(1);
        doAnswer(invocation -> {
            try {
                stalled.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return null;
        }).when(session).sendMessage(any());
        Outbox outbox = new Outbox(session, 3);

        long start = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            outbox.send("message " + i);
        }

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(1));
        verify(session, timeout(2000)).close(CloseReason.TOO_SLOW.status());
        stalled.countDown();
    }
}
