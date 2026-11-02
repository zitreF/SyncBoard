package me.fertiz.syncboard;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

final class TestClient implements AutoCloseable {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private final BlockingQueue<JsonNode> messages = new LinkedBlockingQueue<>();
    private final CompletableFuture<Integer> closed = new CompletableFuture<>();
    private final WebSocket socket;
    private JsonNode welcome;

    private TestClient(URI uri) {
        this.socket = HTTP.newWebSocketBuilder().buildAsync(uri, new Listener()).join();
    }

    static TestClient connect(int port, String room) {
        return new TestClient(URI.create("ws://localhost:" + port + "/ws/rooms/" + room));
    }

    static TestClient join(int port, String room, String name) throws InterruptedException {
        return join(port, room, name, null);
    }

    static TestClient join(int port, String room, String name, String code) throws InterruptedException {
        TestClient client = connect(port, room);
        client.sendJoin(name, code);
        client.welcome = client.next("welcome");
        return client;
    }

    JsonNode welcome() {
        return welcome;
    }

    void sendJoin(String name, String code) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "join");
        message.put("name", name);
        message.put("code", code);
        send(message);
    }

    void draw(String id, String color, int width, int... points) {
        send(Map.of("type", "draw", "id", id, "color", color, "width", width, "points", points));
    }

    void cursor(Integer x, Integer y) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", "cursor");
        message.put("x", x);
        message.put("y", y);
        send(message);
    }

    void send(Object message) {
        sendRaw(JSON.writeValueAsString(message));
    }

    void sendRaw(String payload) {
        socket.sendText(payload, true).join();
    }

    JsonNode next(String type) throws InterruptedException {
        JsonNode message = messages.poll(5, TimeUnit.SECONDS);
        assertThat(message).as("a %s message within 5 seconds", type).isNotNull();
        assertThat(message.get("type").stringValue()).as("type of %s", message).isEqualTo(type);
        return message;
    }

    void expectNothingFor(Duration duration) throws InterruptedException {
        assertThat(messages.poll(duration.toMillis(), TimeUnit.MILLISECONDS)).isNull();
    }

    int closeCode() throws Exception {
        return closed.get(5, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        if (!socket.isOutputClosed()) {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "").exceptionally(error -> null).join();
        }
    }

    private final class Listener implements WebSocket.Listener {

        private final StringBuilder partial = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partial.append(data);
            if (last) {
                messages.add(JSON.readTree(partial.toString()));
                partial.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            closed.complete(statusCode);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            closed.completeExceptionally(error);
        }
    }
}
