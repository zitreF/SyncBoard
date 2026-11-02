package me.fertiz.syncboard.room;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    static final int MIN_CODE_LENGTH = 4;
    static final int MAX_CODE_LENGTH = 32;

    public record CreateRoom(String code) {}

    public record RoomInfo(String id, boolean locked) {

        static RoomInfo of(Room room) {
            return new RoomInfo(room.id(), room.isLocked());
        }
    }

    private final RoomService rooms;

    public RoomController(RoomService rooms) {
        this.rooms = rooms;
    }

    @PostMapping
    public ResponseEntity<RoomInfo> create(@RequestBody(required = false) CreateRoom request) {
        String code = request == null || request.code() == null || request.code().isBlank() ? null : request.code().strip();
        if (code != null && (code.length() < MIN_CODE_LENGTH || code.length() > MAX_CODE_LENGTH)) {
            return ResponseEntity.badRequest().build();
        }
        return rooms.create(code)
                .map(room -> ResponseEntity.status(HttpStatus.CREATED).body(RoomInfo.of(room)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomInfo> find(@PathVariable String id) {
        return ResponseEntity.of(rooms.find(id).map(RoomInfo::of));
    }
}
