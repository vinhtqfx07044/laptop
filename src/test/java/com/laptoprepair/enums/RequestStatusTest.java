package com.laptoprepair.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestStatusTest {

    @Test
    void canTransitionTo_VariousStates_ShouldReturnExpectedBoolean() {
        // SCHEDULED can transition to any status after it
        assertThat(RequestStatus.SCHEDULED.canTransitionTo(RequestStatus.QUOTED)).isTrue();
        assertThat(RequestStatus.SCHEDULED.canTransitionTo(RequestStatus.APPROVE_QUOTED)).isTrue();
        assertThat(RequestStatus.SCHEDULED.canTransitionTo(RequestStatus.IN_PROGRESS)).isTrue();
        assertThat(RequestStatus.SCHEDULED.canTransitionTo(RequestStatus.COMPLETED)).isTrue();
        assertThat(RequestStatus.SCHEDULED.canTransitionTo(RequestStatus.CANCELLED)).isTrue();

        // QUOTED can transition to any status after it
        assertThat(RequestStatus.QUOTED.canTransitionTo(RequestStatus.APPROVE_QUOTED)).isTrue();
        assertThat(RequestStatus.QUOTED.canTransitionTo(RequestStatus.IN_PROGRESS)).isTrue();
        assertThat(RequestStatus.QUOTED.canTransitionTo(RequestStatus.COMPLETED)).isTrue();
        assertThat(RequestStatus.QUOTED.canTransitionTo(RequestStatus.CANCELLED)).isTrue();
        assertThat(RequestStatus.QUOTED.canTransitionTo(RequestStatus.SCHEDULED)).isFalse();

        // APPROVE_QUOTED can transition to status after it
        assertThat(RequestStatus.APPROVE_QUOTED.canTransitionTo(RequestStatus.IN_PROGRESS)).isTrue();
        assertThat(RequestStatus.APPROVE_QUOTED.canTransitionTo(RequestStatus.COMPLETED)).isTrue();
        assertThat(RequestStatus.APPROVE_QUOTED.canTransitionTo(RequestStatus.CANCELLED)).isTrue();
        assertThat(RequestStatus.APPROVE_QUOTED.canTransitionTo(RequestStatus.QUOTED)).isFalse();

        // IN_PROGRESS can transition to COMPLETED or CANCELLED
        assertThat(RequestStatus.IN_PROGRESS.canTransitionTo(RequestStatus.COMPLETED)).isTrue();
        assertThat(RequestStatus.IN_PROGRESS.canTransitionTo(RequestStatus.CANCELLED)).isTrue();
        assertThat(RequestStatus.IN_PROGRESS.canTransitionTo(RequestStatus.APPROVE_QUOTED)).isFalse();

        // COMPLETED cannot transition to any state
        assertThat(RequestStatus.COMPLETED.canTransitionTo(RequestStatus.CANCELLED)).isFalse();
        assertThat(RequestStatus.COMPLETED.canTransitionTo(RequestStatus.SCHEDULED)).isFalse();
        assertThat(RequestStatus.COMPLETED.canTransitionTo(RequestStatus.IN_PROGRESS)).isFalse();

        // CANCELLED cannot transition to any state
        assertThat(RequestStatus.CANCELLED.canTransitionTo(RequestStatus.COMPLETED)).isFalse();
        assertThat(RequestStatus.CANCELLED.canTransitionTo(RequestStatus.SCHEDULED)).isFalse();
    }

    @Test
    void isRequestItemsLocked_ForCompletedAndScheduled_ShouldReturnTrueFalse() {
        // COMPLETED should be locked
        assertThat(RequestStatus.COMPLETED.isRequestItemsLocked()).isTrue();

        // SCHEDULED should not be locked
        assertThat(RequestStatus.SCHEDULED.isRequestItemsLocked()).isFalse();

        // APPROVE_QUOTED should be locked
        assertThat(RequestStatus.APPROVE_QUOTED.isRequestItemsLocked()).isTrue();

        // QUOTED should not be locked
        assertThat(RequestStatus.QUOTED.isRequestItemsLocked()).isFalse();
    }
}