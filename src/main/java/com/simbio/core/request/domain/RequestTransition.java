package com.simbio.core.request.domain;

import com.simbio.core.request.api.RequestStatus;

public record RequestTransition(RequestStatus from, RequestStatus to) {
}
