package com.simbio.core.request.domain;

import com.simbio.core.request.api.RequestStatus;

public class InvalidRequestTransitionException extends IllegalStateException {
    public InvalidRequestTransitionException(RequestStatus from, RequestStatus to) {
        super("Invalid request transition from " + from + " to " + to);
    }
}
