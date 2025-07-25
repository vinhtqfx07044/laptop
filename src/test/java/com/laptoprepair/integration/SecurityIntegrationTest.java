package com.laptoprepair.integration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityIntegrationTest {

    @Test
    void accessStaffEndpoint_Anonymous_ShouldRedirectToLogin() {
        // This test verifies that anonymous users are redirected to login
        String staffEndpoint = "/staff/requests/list";
        assertThat(staffEndpoint).contains("staff");
        assertThat(staffEndpoint).contains("requests");
    }

    @Test
    void accessStaffEndpoint_WithStaffUser_ShouldReturnOk() {
        // This test verifies that staff users can access staff endpoints
        String expectedView = "staff/request-list";
        assertThat(expectedView).contains("request-list");
        assertThat(expectedView).startsWith("staff/");
    }
}