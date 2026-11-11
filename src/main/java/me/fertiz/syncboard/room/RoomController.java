package me.fertiz.syncboard.room;

import me.fertiz.syncboard.room.dto.request.CreateRoomRequest;
import me.fertiz.syncboard.room.dto.response.RoomResponse;
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
    public ResponseEntity<RoomResponse> create(@RequestBody CreateRoomRequest request) {
        if (!request.isValid()) {
            return ResponseEntity.badRequest().build();
        }
        return rooms.create(request.code())
                .map(RoomResponse::from)
                .map(room -> ResponseEntity.status(HttpStatus.CREATED).body(room))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomResponse> find(@PathVariable String id) {
        return ResponseEntity.of(rooms.find(id).map(RoomResponse::from));
    }
}
