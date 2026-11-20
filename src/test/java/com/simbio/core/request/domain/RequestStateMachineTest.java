package com.simbio.core.request.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.simbio.core.request.api.RequestStatus;

class RequestStateMachineTest {
    @Test
    @DisplayName("TC-REQ-003: accepted custom transitions follow the complete state table")
    void customDevelopmentTransitions() {
        assertThatCode(() -> CustomDevelopmentStateMachine.requireTransition(
                RequestStatus.RECIBIDA, RequestStatus.EN_ANALISIS, null, null)).doesNotThrowAnyException();
        assertThatCode(() -> CustomDevelopmentStateMachine.requireTransition(
                RequestStatus.PRESUPUESTO_ENVIADO, RequestStatus.ACEPTADA, null, null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("TC-REQ-003: paused requests resume only their saved state")
    void pauseResumesSavedStateOnly() {
        assertThatCode(() -> CustomDevelopmentStateMachine.requireTransition(
                RequestStatus.EN_PAUSA, RequestStatus.EN_DESARROLLO, null, RequestStatus.EN_DESARROLLO))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> CustomDevelopmentStateMachine.requireTransition(
                RequestStatus.EN_PAUSA, RequestStatus.EN_ANALISIS, null, RequestStatus.EN_DESARROLLO))
                .isInstanceOf(InvalidRequestTransitionException.class);
    }

    @Test
    @DisplayName("TC-REQ-003: terminal custom requests reject new transitions")
    void closedCustomRequestIsTerminal() {
        assertThatThrownBy(() -> CustomDevelopmentStateMachine.requireTransition(
                RequestStatus.CERRADA, RequestStatus.EN_ANALISIS, null, null))
                .isInstanceOf(InvalidRequestTransitionException.class);
    }

    @Test
    @DisplayName("TC-REQ-004: module discard requires an internal reason")
    void moduleDiscardRequiresReason() {
        assertThatThrownBy(() -> SimbiosphereModuleStateMachine.requireTransition(
                RequestStatus.EN_ESTUDIO, RequestStatus.DESCARTADA, "", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("TC-REQ-004: duplicate module requires the original reference")
    void moduleDuplicateRequiresOriginal() {
        assertThatThrownBy(() -> SimbiosphereModuleStateMachine.requireTransition(
                RequestStatus.EN_ESTUDIO, RequestStatus.DUPLICADA, null, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
