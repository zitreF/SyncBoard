# SyncBoard

A small real-time whiteboard. Create a board, send the link to friends (optionally protected with a join code) and
draw together: everyone sees the others' strokes and cursors as they happen.

Spring Boot 4 with a plain WebSocket endpoint on the server, and a vanilla JavaScript canvas page with no build step.

## Running

```bash
./gradlew bootRun
```

Open http://localhost:8080, create a board and open its link in another tab or browser. Java 25 is the only
requirement.

| Key / button   | Action                               |
|----------------|--------------------------------------|
| `Ctrl+Z`       | Undo your last stroke                |
| Trash icon     | Clear the board for everyone         |
| Copy link      | Copy the board's link to share       |

## How it works

1. `POST /api/rooms` creates a room with a random 8-character id and an optional join code (4 to 32 characters).
   `GET /api/rooms/{id}` tells the page whether the room exists and whether it needs a code.
2. The page opens a WebSocket to `/ws/rooms/{id}` and sends `join` with a name and the code. The server answers with
   `welcome`: everything drawn so far and who is on the board. A wrong code closes the socket with `4403`, an unknown
   room with `4404`.
3. While you draw, new points go out every 40 ms as `draw` messages with the stroke's id. The server appends them to
   the stroke and passes them on, so others see the line grow live. A late joiner gets the whole strokes in `welcome`.
4. `erase` (undo) and `clear` work the same way. Cursor positions are only passed on, never stored.

| Client → server                   | Server → client                                  |
|-----------------------------------|--------------------------------------------------|
| `join {name, code}`               | `welcome {you, members, strokes}`                |
| `draw {id, color, width, points}` | `draw`, `erase`, `clear {by}` from the others    |
| `erase {id}`, `clear`             | `members {members}` when someone joins or leaves |
| `cursor {x, y}`                   | `cursor {member, x, y}`                          |
|                                   | `notice {message}`, e.g. when the board is full  |

Some details:

- Each room has one lock, and a change is sent to everyone while it is held, so all clients see changes in the same
  order as the server.
- Every connection is wrapped in Spring's `ConcurrentWebSocketSessionDecorator`, so one slow client cannot hold up a
  room: a client more than 5 seconds or 1 MB behind is disconnected.
- Rooms live in memory. A room that has been empty for 30 minutes is removed.
- Everything a client sends is checked: colors, widths, coordinates, ids, at most 10 000 strokes per board and 20
  people per room. Names are cleaned on the server and only ever put on the page as text.
- The page reconnects on its own after a dropped connection and gets the current board again.

## Tests

`./gradlew build` runs them: unit tests for rooms and message validation, and integration tests that start the app and
talk to it with real WebSocket clients (JDK `HttpClient`): two people drawing, a late joiner, undo and clear, members
and cursors, join codes, unknown rooms and malformed messages.

## Layout

```
src/main/java/me/fertiz/syncboard/room     rooms, strokes, members and the REST endpoint
src/main/java/me/fertiz/syncboard/socket   WebSocket handler and messages
src/main/resources/static                  the pages: HTML, CSS and JavaScript
```
