package me.fertiz.syncboard.api;

import me.fertiz.syncboard.room.RoomService;
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

    private final RoomService rooms;

    public RoomController(RoomService rooms) {
        this.rooms = rooms;
    }

    @PostMapping
    public ResponseEntity<RoomResponse> create(@RequestBody(required = false) CreateRoomRequest request) {
        String code = request == null ? null : request.normalizedCode();
        if (!CreateRoomRequest.isValidCode(code)) {
            return ResponseEntity.badRequest().build();
        }
        return rooms.create(code)
                .map(room -> ResponseEntity.status(HttpStatus.CREATED).body(RoomResponse.from(room)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> find(@PathVariable String id) {
        return ResponseEntity.of(rooms.find(id).map(RoomResponse::from));
    }
}
