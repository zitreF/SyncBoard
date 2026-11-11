package me.fertiz.syncboard.board;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class Outbox {

    private static final Logger LOGGER = LoggerFactory.getLogger(Outbox.class);

    private final WebSocketSession session;
    private final int limit;
    private final ExecutorService sender;

    public Outbox(WebSocketSession session, int limit) {
        this.session = session;
        this.limit = limit;
        this.sender = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(limit),
                Thread.ofVirtual().name("outbox-" + session.getId()).factory());
    }

    public void send(String payload) {
        submit(() -> write(payload));
    }

    public void close(CloseReason reason) {
        submit(() -> closeSession(reason));
    }

    public void stop() {
        sender.shutdownNow();
    }

    private void submit(Runnable task) {
        try {
            sender.execute(task);
        } catch (RejectedExecutionException e) {
            closeAsTooSlow();
        }
    }

    private void closeAsTooSlow() {
        if (sender.isShutdown()) {
            return;
        }
        LOGGER.info("Closing {}: more than {} messages behind", session.getId(), limit);
        sender.shutdownNow();
        Thread.ofVirtual().start(() -> closeSession(CloseReason.TOO_SLOW));
    }

    private void write(String payload) {
        try {
            session.sendMessage(new TextMessage(payload));
        } catch (IOException | IllegalStateException e) {
            LOGGER.debug("Could not send to {}: {}", session.getId(), e.getMessage());
        }
    }

    private void closeSession(CloseReason reason) {
        try {
            session.close(reason.status());
        } catch (IOException | IllegalStateException e) {
            LOGGER.debug("Could not close {}: {}", session.getId(), e.getMessage());
        }
    }
}
