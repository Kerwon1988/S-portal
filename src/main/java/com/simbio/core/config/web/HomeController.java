package com.simbio.core.config.web;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.LocaleResolver;

@Controller
public class HomeController {

    private final LocaleResolver localeResolver;

    public HomeController(LocaleResolver localeResolver) {
        this.localeResolver = localeResolver;
    }

    @GetMapping("/")
    public String root(HttpServletRequest request) {
        return "redirect:/" + localeResolver.resolveLocale(request).getLanguage();
    }

    @GetMapping("/{lang:es|en}")
    public String home(@PathVariable String lang) {
        return "home";
    }
}
