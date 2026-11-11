package me.fertiz.syncboard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BoardSocketTest {

    private static final Duration QUIET = Duration.ofMillis(300);
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    int port;

    @Test
    void createsAnOpenRoomAndJoinsIt() throws Exception {
        String room = createRoom(null);

        try (TestClient alice = TestClient.join(port, room, "Alice")) {
            JsonNode welcome = alice.welcome();
            assertThat(welcome.get("you").stringValue()).isEqualTo("m1");
            assertThat(welcome.get("members").toString()).isEqualTo("[{\"id\":\"m1\",\"name\":\"Alice\",\"color\":\"#e03131\"}]");
            assertThat(welcome.get("strokes").size()).isZero();
        }
    }

    @Test
    void strokesReachTheOthersAndLaterJoiners() throws Exception {
        String room = createRoom(null);
        try (TestClient alice = TestClient.join(port, room, "Alice");
             TestClient bob = TestClient.join(port, room, "Bob")) {
            alice.next("members");

            alice.draw("s1", "#1971c2", 4, 10, 10, 20, 20);
            alice.draw("s1", "#1971c2", 4, 30, 30);

            JsonNode first = bob.next("draw");
            assertThat(first.get("id").stringValue()).isEqualTo("s1");
            assertThat(first.get("color").stringValue()).isEqualTo("#1971c2");
            assertThat(first.get("width").intValue()).isEqualTo(4);
            assertThat(first.get("points").toString()).isEqualTo("[10,10,20,20]");
            assertThat(bob.next("draw").get("points").toString()).isEqualTo("[30,30]");
            alice.expectNothingFor(QUIET);

            try (TestClient carol = TestClient.join(port, room, "Carol")) {
                assertThat(carol.welcome().get("strokes").toString())
                        .isEqualTo("[{\"id\":\"s1\",\"color\":\"#1971c2\",\"width\":4,\"points\":[10,10,20,20,30,30]}]");
            }
        }
    }

    @Test
    void erasingAndClearingReachTheOthers() throws Exception {
        String room = createRoom(null);
        try (TestClient alice = TestClient.join(port, room, "Alice");
             TestClient bob = TestClient.join(port, room, "Bob")) {
            alice.next("members");
            alice.draw("s1", "#1e1e1e", 2, 1, 1);
            alice.draw("s2", "#1e1e1e", 2, 2, 2);
            bob.next("draw");
            bob.next("draw");

            alice.send(Map.of("type", "erase", "id", "s1"));
            assertThat(bob.next("erase").get("id").stringValue()).isEqualTo("s1");

            bob.send(Map.of("type", "clear"));
            assertThat(alice.next("clear").get("by").stringValue()).isEqualTo("m2");

            try (TestClient carol = TestClient.join(port, room, "Carol")) {
                assertThat(carol.welcome().get("strokes").size()).isZero();
            }
        }
    }

    @Test
    void membersAndCursorsAreShared() throws Exception {
        String room = createRoom(null);
        try (TestClient alice = TestClient.join(port, room, "Alice")) {
            TestClient bob = TestClient.join(port, room, "Bob");
            assertThat(names(alice.next("members"))).isEqualTo("[Alice, Bob]");

            alice.cursor(5, 6);
            JsonNode cursor = bob.next("cursor");
            assertThat(cursor.get("member").stringValue()).isEqualTo("m1");
            assertThat(cursor.get("x").intValue()).isEqualTo(5);
            assertThat(cursor.get("y").intValue()).isEqualTo(6);

            alice.cursor(null, null);
            assertThat(bob.next("cursor").get("x").isNull()).isTrue();

            bob.close();
            assertThat(names(alice.next("members"))).isEqualTo("[Alice]");
        }
    }

    @Test
    void aLockedRoomNeedsItsCode() throws Exception {
        String room = createRoom("secret-word");
        assertThat(get("/api/rooms/" + room).body()).isEqualTo("{\"id\":\"" + room + "\",\"locked\":true}");

        try (TestClient guess = TestClient.connect(port, room)) {
            guess.sendJoin("Mallory", "secret");
            assertThat(guess.closeCode()).isEqualTo(4403);
        }
        try (TestClient noCode = TestClient.connect(port, room)) {
            noCode.sendJoin("Mallory", null);
            assertThat(noCode.closeCode()).isEqualTo(4403);
        }
        try (TestClient alice = TestClient.join(port, room, "Alice", "secret-word")) {
            assertThat(alice.welcome().get("you").stringValue()).isEqualTo("m1");
        }
    }

    @Test
    void joinCodesNeedASensibleLength() throws Exception {
        assertThat(post("{\"code\":\"abc\"}").statusCode()).isEqualTo(400);
        assertThat(post("{\"code\":\"" + "x".repeat(33) + "\"}").statusCode()).isEqualTo(400);

        HttpResponse<String> blank = post("{\"code\":\"   \"}");
        assertThat(blank.statusCode()).isEqualTo(201);
        assertThat(JSON.readTree(blank.body()).get("locked").booleanValue()).isFalse();
    }

    @Test
    void unknownRoomsAreRefused() throws Exception {
        assertThat(get("/api/rooms/zzzzzzzz").statusCode()).isEqualTo(404);

        try (TestClient client = TestClient.connect(port, "zzzzzzzz")) {
            assertThat(client.closeCode()).isEqualTo(4404);
        }
    }

    @Test
    void anythingButJoinComesTooEarly() throws Exception {
        String room = createRoom(null);

        try (TestClient client = TestClient.connect(port, room)) {
            client.draw("s1", "#1e1e1e", 2, 1, 1);
            assertThat(client.closeCode()).isEqualTo(4400);
        }
    }

    @Test
    void malformedMessagesCloseTheConnection() throws Exception {
        String room = createRoom(null);

        try (TestClient client = TestClient.join(port, room, "Alice")) {
            client.sendRaw("{not json");
            assertThat(client.closeCode()).isEqualTo(4400);
        }
        try (TestClient client = TestClient.join(port, room, "Alice")) {
            client.sendRaw("null");
            assertThat(client.closeCode()).isEqualTo(4400);
        }
        try (TestClient client = TestClient.join(port, room, "Alice")) {
            client.draw("s1", "red", 2, 1, 1);
            assertThat(client.closeCode()).isEqualTo(4400);
        }
        try (TestClient client = TestClient.join(port, room, "Alice")) {
            client.sendJoin("Alice again", null);
            assertThat(client.closeCode()).isEqualTo(4400);
        }
    }

    @Test
    void servesTheHomeAndBoardPages() throws Exception {
        String room = createRoom(null);

        assertThat(get("/").body()).contains("Create a board");
        HttpResponse<String> board = get("/r/" + room);
        assertThat(board.statusCode()).isEqualTo(200);
        assertThat(board.body()).contains("/js/board.js");
    }

    @Test
    void tellsTheBrowserToCheckForNewerScripts() throws Exception {
        HttpResponse<String> script = get("/js/board.js");

        assertThat(script.statusCode()).isEqualTo(200);
        assertThat(script.headers().firstValue("Cache-Control")).contains("no-cache");
    }

    private String createRoom(String code) throws IOException, InterruptedException {
        HttpResponse<String> response = post(code == null ? "{}" : "{\"code\":\"" + code + "\"}");
        assertThat(response.statusCode()).isEqualTo(201);
        return JSON.readTree(response.body()).get("id").stringValue();
    }

    private HttpResponse<String> post(String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri("/api/rooms"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return HTTP.send(HttpRequest.newBuilder(uri(path)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static String names(JsonNode message) {
        List<String> names = new ArrayList<>();
        for (JsonNode member : message.get("members")) {
            names.add(member.get("name").stringValue());
        }
        return names.toString();
    }
}
