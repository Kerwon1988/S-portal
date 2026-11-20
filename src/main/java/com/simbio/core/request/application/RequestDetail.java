package com.simbio.core.request.application;

import java.util.List;
import java.util.UUID;

import com.simbio.core.request.api.RequestSummary;
import com.simbio.core.request.domain.RequestStatusHistoryEntity;

public record RequestDetail(RequestSummary summary, String description, String objective, String impact,
        String relatedModule, String decisionReason, String duplicateReference,
        List<RequestStatusHistoryEntity> history, List<UUID> attachments) {
}
