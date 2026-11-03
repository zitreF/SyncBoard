package me.fertiz.syncboard.room;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class RoomPageController {

    @GetMapping("/r/{room:[a-z0-9]{8}}")
    public String board(@PathVariable String room) {
        return "forward:/board.html";
    }
}
