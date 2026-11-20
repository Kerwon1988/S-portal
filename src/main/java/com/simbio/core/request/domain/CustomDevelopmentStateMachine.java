package com.simbio.core.request.domain;

import java.util.Set;

import com.simbio.core.request.api.RequestStatus;

public final class CustomDevelopmentStateMachine {
    private static final Set<RequestTransition> ALLOWED = Set.of(
            new RequestTransition(RequestStatus.RECIBIDA, RequestStatus.EN_ANALISIS),
            new RequestTransition(RequestStatus.EN_ANALISIS, RequestStatus.PENDIENTE_INFO_CLIENTE),
            new RequestTransition(RequestStatus.PENDIENTE_INFO_CLIENTE, RequestStatus.EN_ANALISIS),
            new RequestTransition(RequestStatus.EN_ANALISIS, RequestStatus.PRESUPUESTO_ENVIADO),
            new RequestTransition(RequestStatus.PRESUPUESTO_ENVIADO, RequestStatus.ACEPTADA),
            new RequestTransition(RequestStatus.PRESUPUESTO_ENVIADO, RequestStatus.RECHAZADA),
            new RequestTransition(RequestStatus.ACEPTADA, RequestStatus.EN_DESARROLLO),
            new RequestTransition(RequestStatus.EN_DESARROLLO, RequestStatus.EN_REVISION_CLIENTE),
            new RequestTransition(RequestStatus.EN_REVISION_CLIENTE, RequestStatus.EN_DESARROLLO),
            new RequestTransition(RequestStatus.EN_REVISION_CLIENTE, RequestStatus.ENTREGADA),
            new RequestTransition(RequestStatus.ENTREGADA, RequestStatus.CERRADA),
            new RequestTransition(RequestStatus.EN_ANALISIS, RequestStatus.EN_PAUSA),
            new RequestTransition(RequestStatus.EN_DESARROLLO, RequestStatus.EN_PAUSA),
            new RequestTransition(RequestStatus.RECIBIDA, RequestStatus.CANCELADA),
            new RequestTransition(RequestStatus.EN_ANALISIS, RequestStatus.CANCELADA),
            new RequestTransition(RequestStatus.EN_PAUSA, RequestStatus.CANCELADA));

    public static void requireTransition(RequestStatus from, RequestStatus to, String reason,
            RequestStatus pausedFromStatus) {
        if (from == RequestStatus.EN_PAUSA
                && (to == RequestStatus.EN_ANALISIS || to == RequestStatus.EN_DESARROLLO)) {
            if (to == pausedFromStatus) {
                return;
            }
            throw new InvalidRequestTransitionException(from, to);
        }
        if (!ALLOWED.contains(new RequestTransition(from, to))) {
            throw new InvalidRequestTransitionException(from, to);
        }
        if ((to == RequestStatus.PENDIENTE_INFO_CLIENTE || to == RequestStatus.EN_PAUSA)
                && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Transition reason is required");
        }
    }

    private CustomDevelopmentStateMachine() {
    }
}
