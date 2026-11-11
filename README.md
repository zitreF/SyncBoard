# SyncBoard

[![Build](https://github.com/zitreF/syncboard/actions/workflows/build.yml/badge.svg)](https://github.com/zitreF/syncboard/actions/workflows/build.yml)

A small real-time whiteboard. Create a board, send the link to friends (optionally protected with a join code) and
draw together: everyone sees the others' strokes and cursors as they happen.

![Three people on one board: Ola and Kuba's cursors with their names, the list of people and the toolbar](docs/board.png)

## Features

- **Boards with a link.** One click creates a board with a short random id, for example `/r/s8i3eck9`. Anyone with the
  link can join.
- **Optional join code.** A board can be locked with a code of 4 to 32 characters. Without it the server refuses to
  let anyone in.
- **Live drawing.** Five colors, three widths. Others see a line grow while it is being drawn, not only after the
  mouse is released.
- **Live cursors and people.** Everyone's cursor moves on the board with their name next to it, and the top right
  corner shows who is on the board.
- **Undo and clear.** `Ctrl+Z` removes your own last stroke, the trash icon clears the board for everyone.
- **Late joiners and reconnects.** Someone who joins later gets everything drawn so far. A page that loses the
  connection reconnects on its own and gets the current board again.

<p>
  <img src="docs/home.png" width="49%" alt="Home page: create a board with an optional join code">
  <img src="docs/join.png" width="49%" alt="Joining a locked board: name and join code">
</p>

## Running

Java 25 is the only requirement. Gradle comes with the wrapper.

```bash
./gradlew bootRun
```

Open http://localhost:8080, create a board and open its link in another tab or browser. To build a single jar and run
it on another port:

```bash
./gradlew bootJar
PORT=9000 java -jar build/libs/syncboard.jar
```

Settings in `src/main/resources/application.yml`:

| Setting                       | Default | Meaning                                         |
|-------------------------------|---------|-------------------------------------------------|
| `server.port` (env `PORT`)    | `8080`  | HTTP and WebSocket port                         |
| `syncboard.max-rooms`         | `1000`  | how many boards may exist at the same time      |
| `syncboard.room-idle-timeout` | `30m`   | how long an empty board is kept before removal  |
| `syncboard.cleanup-interval`  | `1m`    | how often empty boards are looked for           |

## How it works

The server is a Spring Boot app with one REST controller and one WebSocket handler (`TextWebSocketHandler`, no STOMP).
The page is plain HTML, CSS and JavaScript served by the same app, drawn on a `<canvas>`, with no framework and no
build step.

1. `POST /api/rooms` creates a room with a random 8-character id and an optional join code.
   `GET /api/rooms/{id}` tells the page whether the room exists and whether it needs a code.
2. The page opens a WebSocket to `/ws/rooms/{id}` and sends `join` with a name and the code. The server answers with
   `welcome`: everything drawn so far and who is on the board.
3. While you draw, the page collects points and sends the new ones every 40 ms as a `draw` message with the stroke's
   id. The server appends them to the stroke and passes them on to everyone else.
4. `erase` (undo) and `clear` work the same way. Cursor positions are sent at most every 40 ms and are only passed
   on, never stored.

| Page → server                     | Server → page                                    |
|-----------------------------------|--------------------------------------------------|
| `join {name, code}`               | `welcome {you, members, strokes}`                |
| `draw {id, color, width, points}` | `draw`, `erase`, `clear {by}` from the others    |
| `erase {id}`, `clear`             | `members {members}` when someone joins or leaves |
| `cursor {x, y}`                   | `cursor {member, x, y}`                          |
|                                   | `notice {message}`, e.g. when the board is full  |

When something is wrong the server closes the socket with a code the page understands: `4403` wrong join code,
`4404` no such board (also one that has expired), `4409` board full (20 people), `4400` a message that breaks the
rules. A client that falls too far behind gets `1013` and the page simply reconnects.

A few decisions worth knowing:

- **One queue per room, no locks.** Each room has its own task queue worked off by a single virtual thread. The
  state of a room (its `Drawing` and `RoomMembers`) can only be reached inside that queue, through
  `Room.execute(state -> ...)`, so it is only ever changed by one thread and needs no `synchronized` or `Lock`.
  Changes and the messages about them go out in queue order, so every page sees them in the same order as the server.
  Rooms never wait for each other.
- **One outbox per connection.** The room thread does not write to sockets itself: it drops each message into the
  connection's `Outbox`, a bounded queue with its own virtual thread. A client whose connection stalls cannot hold up
  its room; once it is 1000 messages behind it is disconnected.
- **Nothing from the client is trusted.** Colors, widths, coordinates and ids are checked, and names are cleaned on the
  server and only ever put on the page as text. A board holds at most 10 000 strokes and 200 000 coordinates, so one
  person cannot fill the server's memory; when it is full the page says so.
- **Cheap on the hot path.** A stroke keeps its points in a plain `int[]`, a message is turned into JSON once however
  many people get it, and the page draws only the new piece of a line instead of repainting the whole board. It
  repaints everything only when the order of strokes could change: undo, clear, resize.
- **Boards live in memory.** That keeps the project small. The trade-off: restarting the server empties every board,
  and one instance is the limit.

What it does not do: the join code is a simple lock for a board shared among friends, not real access control. There
are no accounts and no limit on how often someone may guess a code, create boards or move a cursor.

## Tests

```bash
./gradlew build
```

- `DrawingTest`, `StrokeTest`: strokes, undo and clear, and the limits of one board.
- `RoomMembersTest`, `MemberTest`: ids and colors, leaving, the member limit, name cleaning and when a room counts as
  empty.
- `RoomTest`, `RoomServiceTest`: join codes, removing empty boards (with a fake clock) and the room queue: tasks run in
  order on one thread, and 20 people drawing into one stroke at the same time lose no points.
- `BoardServiceTest`: joining, a board that does not exist and one that expired while someone was connected.
- `DrawMessageTest`, `CursorMessageTest`: which messages are accepted.
- `MessageCodecTest`: every message reads and writes with the type the page expects.
- `OutboxTest`: messages go out in order, and a stalled client is closed without making the room wait.
- `BoardSocketTest`: starts the app on a random port and talks to it with real WebSocket clients (JDK `HttpClient`):
  two people drawing, a late joiner, undo and clear, members and cursors, locked boards, unknown boards, malformed
  messages and the cache header on the scripts.

GitHub Actions runs the same build on every push.

## Project layout

The code follows the usual Spring Boot layout: one package per feature, holding its controller, service and repository
side by side, with the objects that go over the wire in `dto/request` and `dto/response`. A new feature gets its own
package next to these.

```
src/main/java/me/fertiz/syncboard
    SyncBoardApplication            starts the app
    room/                           creating, finding and expiring rooms
        Room                        one room: its id, join code and task queue
        RoomState                   what the queue protects: the drawing and the members
        Drawing, Stroke             the lines on the board and their limits
        RoomMembers, Member         who is in the room
        RoomController              POST /api/rooms, GET /api/rooms/{id}
        RoomPageController          serves the board page at /r/{id}
        RoomService                 creates rooms, removes empty ones
        RoomRepository              keeps the rooms in memory
        RoomIdGenerator             random room ids
        RoomProperties              the settings from application.yml
        dto/request, dto/response   CreateRoomRequest, RoomResponse
    board/                          drawing together live over WebSocket
        BoardConnection             one connected page and its send queue (Outbox)
        BoardSocketHandler          WebSocket events: opens connections, reads and checks messages
        BoardService                what each message does to a room
        BoardConnectionRepository   keeps the open connections
        BoardBroadcaster            sends messages to the people in a room
        CloseReason, MessageCodec   close codes, JSON in and out
        BoardSocketConfig           registers /ws/rooms/{id}
        dto/request, dto/response   one record per message in each direction
src/main/resources/static
    index.html, board.html          the home page and the board
    css/style.css
    js/
        home.js                     creates a board
        board.js                    wires the board page together
        join-dialog.js              asking for a name and the join code
        people.js                   the list of people on the board
        notices.js                  status, messages and the "board is gone" panel
        socket.js                   the WebSocket and reconnecting
        canvas.js                   the strokes on the canvas, drawn piece by piece
        pen.js                      the stroke being drawn: sending points, undo
        cursors.js                  other people's cursors
        throttle.js, storage.js     small helpers
```

Tests use the same packages. `BoardSocketTest` in the root package starts the whole app.

## Stack

Java 25, Spring Boot 4.1 (Web MVC, WebSocket), Jackson, JUnit and AssertJ, plain JavaScript with Canvas 2D, Gradle
with the Kotlin DSL.
