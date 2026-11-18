package com.simbio.core.content.application;

import java.util.UUID;

public record TeamMemberDraftView(UUID publicId, int position, boolean active, boolean materialsApproved,
        String locale, String displayName, String roleTitle, String profileUrl) {
}
