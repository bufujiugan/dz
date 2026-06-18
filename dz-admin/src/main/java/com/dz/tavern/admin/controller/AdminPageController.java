package com.dz.tavern.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

    @GetMapping({"/", "/admin", "/admin/"})
    public String home() {
        return "forward:/admin/index.html";
    }
}
