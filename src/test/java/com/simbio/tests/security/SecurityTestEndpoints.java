package com.simbio.tests.security;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SecurityTestEndpoints {

    @GetMapping("/public/headers")
    public String headers() {
        return "ok";
    }

    @GetMapping("/public/csrf")
    public String csrf(CsrfToken csrfToken) {
        return csrfToken.getToken();
    }

    @PostMapping("/public/mutation")
    public String mutation() {
        return "ok";
    }

    @GetMapping("/public/session")
    public String session(HttpServletRequest request) {
        request.getSession();
        return "ok";
    }
}
