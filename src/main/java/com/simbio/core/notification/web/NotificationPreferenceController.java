package com.simbio.core.notification.web;

import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.simbio.core.notification.application.NotificationPreferenceService;
import com.simbio.core.shared.spi.CurrentUser;

@Controller
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferences;
    private final CurrentUser currentUser;

    public NotificationPreferenceController(NotificationPreferenceService preferences, CurrentUser currentUser) {
        this.preferences = preferences;
        this.currentUser = currentUser;
    }

    @GetMapping("/cuenta/notificaciones")
    public String form(Model model) {
        UUID userId = authenticatedUserId();
        var preference = preferences.viewFor(userId);
        model.addAttribute("requestUpdates", preference.requestUpdates());
        model.addAttribute("messageUpdates", preference.messageUpdates());
        return "notification/preferences";
    }

    @PostMapping("/cuenta/notificaciones")
    public String save(
            @RequestParam(name = "requestUpdates", defaultValue = "false") boolean requestUpdates,
            @RequestParam(name = "messageUpdates", defaultValue = "false") boolean messageUpdates,
            Model model) {
        UUID userId = authenticatedUserId();
        preferences.update(userId, requestUpdates, messageUpdates);
        model.addAttribute("requestUpdates", requestUpdates);
        model.addAttribute("messageUpdates", messageUpdates);
        model.addAttribute("saved", true);
        return "notification/preferences";
    }

    private UUID authenticatedUserId() {
        return currentUser.currentUser()
                .map(user -> user.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN));
    }
}
