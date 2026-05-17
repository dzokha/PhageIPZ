package vn.ipz.phageipz.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebViewController {

    @GetMapping("/")
    public String indexPage() {
        return "index"; // return templates/index.html
    }

    @GetMapping("/history")
    public String historyPage() {
        return "history"; // return templates/history.html
    }

    @GetMapping("/view")
    public String viewPage() {
        return "view-raw"; // return templates/history.html
    }
}