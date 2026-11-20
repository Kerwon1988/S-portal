package com.simbio.core.request.domain;

import java.util.Set;

import com.simbio.core.request.api.RequestStatus;

public final class SimbiosphereModuleStateMachine {
    private static final Set<RequestTransition> ALLOWED = Set.of(
            new RequestTransition(RequestStatus.RECIBIDA, RequestStatus.EN_ESTUDIO),
            new RequestTransition(RequestStatus.EN_ESTUDIO, RequestStatus.PLANIFICADA),
            new RequestTransition(RequestStatus.EN_ESTUDIO, RequestStatus.DESCARTADA),
            new RequestTransition(RequestStatus.EN_ESTUDIO, RequestStatus.DUPLICADA),
            new RequestTransition(RequestStatus.PLANIFICADA, RequestStatus.EN_DESARROLLO),
            new RequestTransition(RequestStatus.EN_DESARROLLO, RequestStatus.PUBLICADA),
            new RequestTransition(RequestStatus.RECIBIDA, RequestStatus.CANCELADA));

    public static void requireTransition(RequestStatus from, RequestStatus to, String reason, String originalReference) {
        if (!ALLOWED.contains(new RequestTransition(from, to))) {
            throw new InvalidRequestTransitionException(from, to);
        }
        if (to == RequestStatus.DESCARTADA && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Discard reason is required");
        }
        if (to == RequestStatus.DUPLICADA && (originalReference == null || originalReference.isBlank())) {
            throw new IllegalArgumentException("Original request reference is required");
        }
    }

    private SimbiosphereModuleStateMachine() {
    }
}
