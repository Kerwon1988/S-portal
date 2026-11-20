package com.simbio.core.messaging.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.simbio.core.messaging.api.MessageVisibility;

public record MessagePostForm(
        @Size(max = 10_000, message = "{messaging.thread.body.size}")
        String body,
        @NotNull(message = "{messaging.thread.visibility.required}")
        MessageVisibility visibility,
        @Size(max = 40) String templateCode) {

    public static MessagePostForm empty() {
        return new MessagePostForm("", MessageVisibility.PUBLIC, "");
    }

    public String selectedBody(String templateBody) {
        if (body != null && !body.isBlank()) {
            return body;
        }
        return templateBody;
    }
}
