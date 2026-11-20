package com.simbio.core.request.application;

import java.util.UUID;

public record StaffWorkload(UUID staffUserId, String jobTitle, long openAssignments, long overdue) {
}
