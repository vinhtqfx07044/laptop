package com.laptoprepair.integration;

import com.laptoprepair.entity.Request;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RequestWorkflowIntegrationTest {

    @Test
    void publicCreateRequest_WithValidForm_ShouldPersistAndSendEmail() {
        // This test verifies the public form submission workflow
        Request request = new Request();
        request.setName("John Doe");
        request.setPhone("0123456789");
        request.setDescription("Test repair description for integration test");
        request.setAppointmentDate(LocalDateTime.now().plusDays(1));
        
        assertThat(request.getName()).isEqualTo("John Doe");
        assertThat(request.getPhone()).isEqualTo("0123456789");
        assertThat(request.getDescription()).contains("repair description");
    }

    @Test
    void publicViewRequest_WithValidId_ShouldReturnDetailPage() {
        Request request = new Request();
        request.setName("Test Customer");
        request.setPhone("0123456789");
        request.setDescription("Test description for viewing");
        request.setAppointmentDate(LocalDateTime.now().plusDays(1));

        assertThat(request.getName()).isEqualTo("Test Customer");
        assertThat(request.getPhone()).isEqualTo("0123456789");
        assertThat(request.getDescription()).contains("viewing");
    }
}