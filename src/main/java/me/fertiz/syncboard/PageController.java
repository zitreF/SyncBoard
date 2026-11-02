package me.fertiz.syncboard;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/r/{room:[a-z0-9]{8}}")
    public String board() {
        return "forward:/board.html";
    }
}
