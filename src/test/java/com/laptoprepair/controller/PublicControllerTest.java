package com.laptoprepair.controller;

import com.laptoprepair.service.AuthService;
import com.laptoprepair.service.RequestService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PublicControllerTest {

    @Mock
    private RequestService requestService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private PublicController publicController;

    private MockMvc mockMvc;

    @Test
    void login_WhenStaff_ShouldRedirectToStaffList() throws Exception {
        when(authService.isStaff()).thenReturn(true);
        mockMvc = MockMvcBuilders.standaloneSetup(publicController).build();

        mockMvc.perform(get("/login"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/staff/requests/list"));
    }

    @Test
    void lookupPost_WithValidUUID_ShouldRedirectToDetail() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(publicController).build();
        String validUuid = "550e8400-e29b-41d4-a716-446655440000";

        mockMvc.perform(post("/lookup").param("id", validUuid))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/public/request/" + validUuid));
    }

    @Test
    void lookupPost_WithInvalidUUID_ShouldRedirectHomeWithError() throws Exception {
        mockMvc = MockMvcBuilders.standaloneSetup(publicController).build();

        mockMvc.perform(post("/lookup").param("id", "invalid-uuid"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attribute("errorMessage", "ID yêu cầu không hợp lệ. Vui lòng kiểm tra lại."));
    }
}