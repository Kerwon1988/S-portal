package com.simbio.core.request.application;

public record RequestDashboard(long unassigned, long mine, long overdue, long awaitingQuote,
        long unreadPublicMessages) {
}
