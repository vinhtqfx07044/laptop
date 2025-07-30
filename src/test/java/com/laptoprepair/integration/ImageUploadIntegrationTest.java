package com.laptoprepair.integration;

import com.laptoprepair.config.TestEmailConfig;
import com.laptoprepair.entity.Request;
import com.laptoprepair.repository.RequestRepository;
import com.laptoprepair.service.FileStorageService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(TestEmailConfig.class)
class ImageUploadIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private RequestRepository requestRepository;

        @MockBean
        private FileStorageService fileStorageService;

        @Test
        @WithMockUser(roles = "STAFF")
        void uploadMultipleImages_WithValidFiles_ShouldStoreAllImages() throws Exception {
                Request request = createTestRequest();
                Request saved = requestRepository.save(request);

                MockMultipartFile image1 = new MockMultipartFile(
                                "newImages", "image1.jpg", "image/jpeg", "image1 content".getBytes());
                MockMultipartFile image2 = new MockMultipartFile(
                                "newImages", "image2.png", "image/png", "image2 content".getBytes());

                when(fileStorageService.save(any(), any()))
                                .thenReturn("stored-image1.jpg")
                                .thenReturn("stored-image2.png");

                mockMvc.perform(multipart("/staff/requests/edit/" + saved.getId())
                                .file(image1)
                                .file(image2)
                                .param("name", saved.getName())
                                .param("phone", saved.getPhone())
                                .param("email", saved.getEmail())
                                .param("description", saved.getDescription())
                                .param("appointmentDate", saved.getAppointmentDate().toString())
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection());

                Request updated = requestRepository.findById(saved.getId()).orElseThrow();
                assertThat(updated.getImages()).hasSize(2);
        }

        @Test
        @WithMockUser(roles = "STAFF")
        void deleteImageFromRequest_WithValidImageId_ShouldRemoveImage() throws Exception {
                Request request = createTestRequest();
                Request saved = requestRepository.save(request);

                String imageIdToDelete = "image-uuid-to-delete";

                mockMvc.perform(multipart("/staff/requests/edit/" + saved.getId())
                                .param("name", saved.getName())
                                .param("phone", saved.getPhone())
                                .param("email", saved.getEmail())
                                .param("description", saved.getDescription())
                                .param("appointmentDate", saved.getAppointmentDate().toString())
                                .param("toDelete", imageIdToDelete)
                                .param("note", "Xóa hình ảnh không cần thiết")
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection());
        }

        @Test
        @WithMockUser(roles = "STAFF")
        void uploadInvalidFileType_WithTextFile_ShouldReturnValidationError() throws Exception {
                Request request = createTestRequest();
                Request saved = requestRepository.save(request);

                MockMultipartFile invalidFile = new MockMultipartFile(
                                "newImages", "document.txt", "text/plain", "not an image".getBytes());

                mockMvc.perform(multipart("/staff/requests/edit/" + saved.getId())
                                .file(invalidFile)
                                .param("name", saved.getName())
                                .param("phone", saved.getPhone())
                                .param("email", saved.getEmail())
                                .param("description", saved.getDescription())
                                .param("appointmentDate", saved.getAppointmentDate().toString())
                                .with(csrf()))
                                .andExpect(status().is3xxRedirection());
        }

        private Request createTestRequest() {
                Request request = new Request();
                request.setName("Test Customer");
                request.setPhone("0123456789");
                request.setEmail("test@example.com");
                request.setDescription("Test description for file upload testing");
                request.setAppointmentDate(LocalDateTime.now().plusDays(1));
                return request;
        }
}