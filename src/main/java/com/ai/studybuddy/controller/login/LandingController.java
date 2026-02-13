package com.ai.studybuddy.controller.login;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LandingController {

    @GetMapping("/")
    public String home() {
        return "redirect:/landing.html";
    }
}