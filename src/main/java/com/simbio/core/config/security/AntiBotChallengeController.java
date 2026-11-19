package com.simbio.core.config.security;

import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.simbio.core.shared.spi.AntiBotVerification;

@RestController
public class AntiBotChallengeController {

    private final AntiBotVerification antiBotVerifier;

    public AntiBotChallengeController(AntiBotVerification antiBotVerifier) {
        this.antiBotVerifier = antiBotVerifier;
    }

    @GetMapping(value = "/anti-bot/challenge", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> challenge() {
        var challenge = antiBotVerifier.issueChallenge();
        Map<String, Object> body = Map.of(
                "algorithm", challenge.algorithm(),
                "challenge", challenge.challenge(),
                "maxnumber", challenge.maxNumber(),
                "salt", challenge.salt(),
                "signature", challenge.signature());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
