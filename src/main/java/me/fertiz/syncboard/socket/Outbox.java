package me.fertiz.syncboard.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

final class Outbox {

    static final CloseStatus TOO_SLOW = CloseStatus.SERVICE_OVERLOAD.withReason("Too far behind");

    private static final Logger LOGGER = LoggerFactory.getLogger(Outbox.class);

    private final WebSocketSession session;
    private final int limit;
    private final ExecutorService sender;

    Outbox(WebSocketSession session, int limit) {
        this.session = session;
        this.limit = limit;
        this.sender = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(limit),
                Thread.ofVirtual().name("outbox-" + session.getId()).factory());
    }

    void send(String payload) {
        submit(() -> {
            try {
                session.sendMessage(new TextMessage(payload));
            } catch (IOException | IllegalStateException e) {
                LOGGER.debug("Could not send to {}: {}", session.getId(), e.getMessage());
            }
        });
    }

    void close(CloseStatus status) {
        submit(() -> closeSession(status));
    }

    void stop() {
        sender.shutdownNow();
    }

    private void submit(Runnable task) {
        try {
            sender.execute(task);
        } catch (RejectedExecutionException e) {
            if (!sender.isShutdown()) {
                LOGGER.info("Closing {}: more than {} messages behind", session.getId(), limit);
                sender.shutdownNow();
                Thread.ofVirtual().start(() -> closeSession(TOO_SLOW));
            }
        }
    }

    private void closeSession(CloseStatus status) {
        try {
            session.close(status);
        } catch (IOException | IllegalStateException e) {
            LOGGER.debug("Could not close {}: {}", session.getId(), e.getMessage());
        }
    }
}
