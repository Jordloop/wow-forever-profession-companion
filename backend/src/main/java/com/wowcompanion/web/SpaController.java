package com.wowcompanion.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {
    @GetMapping({
            "/overview",
            "/professions",
            "/market",
            "/opportunities",
            "/inventory",
            "/characters",
            "/reference-data",
            "/settings"
    })
    public String forwardWorkspaceRoutesToApplicationShell() {
        return "forward:/index.html";
    }
}
