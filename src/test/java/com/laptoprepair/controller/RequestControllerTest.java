package com.laptoprepair.controller;

import com.laptoprepair.entity.Request;
import com.laptoprepair.service.RequestService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RequestControllerTest {

    @Mock
    private RequestService requestService;

    @InjectMocks
    private RequestController requestController;

    private MockMvc mockMvc;

    @Test
    void list_DefaultParams_ShouldReturnRequestListView() throws Exception {
        ReflectionTestUtils.setField(requestController, "defaultPageSize", 10);
        
        Page<Request> mockPage = new PageImpl<>(List.of(new Request()));
        when(requestService.list(isNull(), isNull(), any(PageRequest.class))).thenReturn(mockPage);
        
        mockMvc = MockMvcBuilders.standaloneSetup(requestController).build();

        mockMvc.perform(get("/staff/requests/list"))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/request-list"))
                .andExpect(model().attributeExists("requests"));
    }

    @Test
    void createForm_ShouldPopulateModelAndReturnFormView() throws Exception {
        ReflectionTestUtils.setField(requestController, "maxImagesPerRequest", 5);
        mockMvc = MockMvcBuilders.standaloneSetup(requestController).build();

        mockMvc.perform(get("/staff/requests/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("staff/request-form"))
                .andExpect(model().attributeExists("request"))
                .andExpect(model().attributeExists("maxImages"));
    }
}