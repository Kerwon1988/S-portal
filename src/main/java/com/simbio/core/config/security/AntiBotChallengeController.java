package com.simbio.core.config.security;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AntiBotChallengeController {

    private final com.simbio.core.shared.spi.AntiBotVerification antiBotVerifier;

    public AntiBotChallengeController(com.simbio.core.shared.spi.AntiBotVerification antiBotVerifier) {
        this.antiBotVerifier = antiBotVerifier;
    }

    @GetMapping("/fragments/anti-bot-challenge")
    public String challenge(Model model) {
        model.addAttribute("challenge", antiBotVerifier.issueChallenge());
        return "fragments/anti-bot :: challenge";
    }
}
