package com.simbio.core.content.api;

import java.util.UUID;

public record TeamMemberView(UUID publicId, String name, String role, SanitizedHtml bio,
        String photoUrl, String photoSrcSet, String photoAlt, String profileUrl) {
}
