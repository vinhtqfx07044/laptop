package com.laptoprepair.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void accessStaffEndpoint_Anonymous_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/staff/requests/list"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void accessStaffEndpoint_WithStaffUser_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/staff/requests/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/request-list"))
                .andExpect(model().attributeExists("requests"));
    }

    @Test
    void accessPublicEndpoint_Anonymous_ShouldReturnOk() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/index"));
    }

    @Test
    void accessLoginPage_Anonymous_ShouldReturnLoginForm() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("public/login"));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void accessLoginPage_AuthenticatedStaff_ShouldRedirectToStaffDashboard() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/requests/list"));
    }

    @Test
    @WithMockUser(roles = "STAFF")
    void accessCreateRequest_WithStaffRole_ShouldReturnForm() throws Exception {
        mockMvc.perform(get("/staff/requests/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/request-form"))
                .andExpect(model().attributeExists("request"));
    }

    @Test
    void accessCreateRequest_Anonymous_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/staff/requests/create"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void accessStaffEndpoint_WithWrongRole_ShouldBeForbidden() throws Exception {
        mockMvc.perform(get("/staff/requests/list"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void accessServiceItemsEndpoint_Anonymous_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/staff/service-items"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }
}