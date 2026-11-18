package com.simbio.core.identity.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class IdentityController {

    @GetMapping("/acceso")
    public String login(@RequestParam(name = "continue", required = false) String continueTo,
            @RequestParam(name = "error", required = false) String error, Model model) {
        model.addAttribute("continueTo", continueTo);
        model.addAttribute("loginFailed", error != null);
        return "identity/login";
    }
}
