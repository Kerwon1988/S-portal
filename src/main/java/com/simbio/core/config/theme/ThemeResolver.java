package com.simbio.core.config.theme;

import java.util.Arrays;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Component;

@Component
public class ThemeResolver {

    public String resolve(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return "system";
        }
        return Arrays.stream(cookies)
                .filter(cookie -> "sp_theme".equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value.equals("light") || value.equals("dark"))
                .findFirst()
                .orElse("system");
    }
}
