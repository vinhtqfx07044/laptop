package com.laptoprepair.integration;

import com.laptoprepair.config.TestEmailConfig;
import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.ServiceItem;
import com.laptoprepair.enums.RequestStatus;
import com.laptoprepair.repository.RequestRepository;
import com.laptoprepair.repository.ServiceItemRepository;
import com.laptoprepair.service.EmailService;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@org.springframework.transaction.annotation.Transactional
@Import(TestEmailConfig.class)
class RequestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private ServiceItemRepository serviceItemRepository;

    @MockBean
    private EmailService emailService;

    @Test
    void publicSubmitRequest_WithValidData_ShouldCreateRequestAndSendEmail() throws Exception {
        long initialCount = requestRepository.count();

        mockMvc.perform(post("/submit")
                .param("name", "John Doe")
                .param("phone", "0123456789")
                .param("email", "john@example.com")
                .param("description", "Laptop không khởi động được, cần kiểm tra")
                .param("appointmentDate", LocalDateTime.now().plusDays(1).toString())
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/submit"))
                .andExpect(flash().attributeExists("successMessage"));

        assertThat(requestRepository.count()).isEqualTo(initialCount + 1);

        Request savedRequest = requestRepository.findAll().get((int) initialCount);
        assertThat(savedRequest.getName()).isEqualTo("John Doe");
        assertThat(savedRequest.getStatus()).isEqualTo(RequestStatus.SCHEDULED);

        verify(emailService).sendConfirmationEmail(any(Request.class));
    }

    @Test
    void publicViewRequest_WithValidId_ShouldReturnRequestDetails() throws Exception {
        Request request = new Request();
        request.setName("Test Customer");
        request.setPhone("0123456789");
        request.setEmail("test@example.com");
        request.setDescription("Test description for viewing request details");
        request.setAppointmentDate(LocalDateTime.now().plusDays(1));

        Request saved = requestRepository.save(request);

        mockMvc.perform(get("/public/request/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/request-detail"))
                .andExpect(model().attributeExists("request"))
                .andExpect(model().attribute("isStaff", false));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void staffUpdateRequestStatus_ShouldTriggerEmailNotification() throws Exception {
        // Create test service item first
        ServiceItem serviceItem = new ServiceItem();
        serviceItem.setName("Test Service");
        serviceItem.setPrice(new BigDecimal("100000"));
        serviceItem.setVatRate(new BigDecimal("0.1"));
        serviceItem.setWarrantyDays(30);
        serviceItem.setActive(true);
        ServiceItem savedServiceItem = serviceItemRepository.save(serviceItem);

        Request request = createTestRequest();
        Request saved = requestRepository.save(request);

        // First transition to QUOTED (valid from SCHEDULED)
        mockMvc.perform(post("/staff/requests/edit/" + saved.getId())
                .param("id", saved.getId().toString())
                .param("name", saved.getName())
                .param("phone", saved.getPhone())
                .param("email", saved.getEmail())
                .param("description", saved.getDescription())
                .param("appointmentDate", saved.getAppointmentDate().toString())
                .param("address", saved.getAddress() != null ? saved.getAddress() : "")
                .param("brandModel", saved.getBrandModel() != null ? saved.getBrandModel() : "")
                .param("status", RequestStatus.QUOTED.name())
                .param("items[0].serviceItemId", savedServiceItem.getId().toString())
                .param("items[0].quantity", "1")
                .param("items[0].price", "100000")
                .param("note", "Đã báo giá cho khách hàng")
                .with(csrf()))
                .andExpect(status().is3xxRedirection());

        Request updated = requestRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(RequestStatus.QUOTED);

        verify(emailService).sendUpdateEmail(any(Request.class), anyString());
    }

    @Test
    void lookupRequest_WithInvalidId_ShouldReturnError() throws Exception {
        mockMvc.perform(post("/lookup")
                .param("id", "invalid-id")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    @Test
    void recoverRequests_WithValidEmail_ShouldSendRecoveryEmail() throws Exception {
        Request request = createTestRequest();
        requestRepository.save(request);

        mockMvc.perform(post("/recover")
                .param("email", "test@example.com")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(emailService).sendRecoverEmail(anyString(), any());
    }

    private Request createTestRequest() {
        Request request = new Request();
        request.setName("Test Customer");
        request.setPhone("0123456789");
        request.setEmail("test@example.com");
        request.setDescription("Test laptop repair request for integration testing");
        request.setAppointmentDate(LocalDateTime.now().plusDays(1));
        request.setAddress("123 Test Street, Test City");
        request.setBrandModel("Dell Inspiron 15");
        return request;
    }
}