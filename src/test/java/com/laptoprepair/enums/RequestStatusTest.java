package com.laptoprepair.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestStatusTest {

    @Test
    void canTransitionTo_VariousStates_ShouldReturnExpectedBoolean() {
        // SCHEDULED can transition to QUOTED or CANCELLED
        assertThat(RequestStatus.SCHEDULED.canTransitionTo(RequestStatus.QUOTED)).isTrue();
        assertThat(RequestStatus.SCHEDULED.canTransitionTo(RequestStatus.CANCELLED)).isTrue();
        assertThat(RequestStatus.SCHEDULED.canTransitionTo(RequestStatus.IN_PROGRESS)).isFalse();

        // COMPLETED cannot transition to any state
        assertThat(RequestStatus.COMPLETED.canTransitionTo(RequestStatus.CANCELLED)).isFalse();
        assertThat(RequestStatus.COMPLETED.canTransitionTo(RequestStatus.SCHEDULED)).isFalse();

        // IN_PROGRESS can transition to COMPLETED or CANCELLED
        assertThat(RequestStatus.IN_PROGRESS.canTransitionTo(RequestStatus.COMPLETED)).isTrue();
        assertThat(RequestStatus.IN_PROGRESS.canTransitionTo(RequestStatus.CANCELLED)).isTrue();
    }

    @Test
    void isLocked_ForCompletedAndScheduled_ShouldReturnTrueFalse() {
        // COMPLETED should be locked
        assertThat(RequestStatus.COMPLETED.isLocked()).isTrue();

        // SCHEDULED should not be locked
        assertThat(RequestStatus.SCHEDULED.isLocked()).isFalse();

        // APPROVE_QUOTED should be locked
        assertThat(RequestStatus.APPROVE_QUOTED.isLocked()).isTrue();

        // QUOTED should not be locked
        assertThat(RequestStatus.QUOTED.isLocked()).isFalse();
    }
}