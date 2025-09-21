package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestImage;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.service.FileStorageService;
import com.laptoprepair.validation.ImageValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceImplTest {

    private ImageValidator imageValidator;

    @Mock
    private FileStorageService fileStorageService;

    private ImageServiceImpl imageService;

    private UUID requestId;
    private Request request;
    private List<RequestImage> currentImages;

    @BeforeEach
    void setUp() {
        requestId = UUID.randomUUID();
        request = new Request();
        request.setId(requestId);
        currentImages = new ArrayList<>();
        
        // Create real ImageValidator with test configuration
        imageValidator = new ImageValidator();
        ReflectionTestUtils.setField(imageValidator, "maxImages", 5);
        
        // Manual dependency injection
        imageService = new ImageServiceImpl(imageValidator, fileStorageService);
    }

    @Test
    void uploadImages_UTC001_ValidNewImagesWithinLimit_ShouldReturnNewRequestImages() throws Exception {
        MultipartFile[] newImages = {
                new MockMultipartFile("file1", "image1.jpg", "image/jpeg", "test content".getBytes()),
                new MockMultipartFile("file2", "image2.jpg", "image/jpeg", "test content".getBytes())
        };

        doNothing().when(fileStorageService).createDir(requestId);
        when(fileStorageService.save(eq(requestId), any(MultipartFile.class)))
                .thenReturn("generated_filename_1.jpg")
                .thenReturn("generated_filename_2.jpg");

        List<RequestImage> result = imageService.uploadImages(requestId, currentImages, newImages, request);

        assertEquals(2, result.size());
        assertEquals("generated_filename_1.jpg", result.get(0).getFilename());
        assertEquals("generated_filename_2.jpg", result.get(1).getFilename());
        assertEquals(request, result.get(0).getRequest());
        assertEquals(request, result.get(1).getRequest());

        verify(fileStorageService).createDir(requestId);
        verify(fileStorageService, times(2)).save(eq(requestId), any(MultipartFile.class));
    }

    @Test
    void uploadImages_UTC002_ExceedMaxImageLimit_ShouldThrowValidationException() throws Exception {
        List<RequestImage> existingImages = createRequestImages(4);
        MultipartFile[] newImages = {
                new MockMultipartFile("file1", "image1.jpg", "image/jpeg", "test content".getBytes()),
                new MockMultipartFile("file2", "image2.jpg", "image/jpeg", "test content".getBytes())
        };

        ValidationException exception = assertThrows(ValidationException.class,
                () -> imageService.uploadImages(requestId, existingImages, newImages, request));

        assertEquals("Tối đa 5 ảnh cho mỗi yêu cầu", exception.getMessage());
        verify(fileStorageService, never()).createDir(any(UUID.class));
        verify(fileStorageService, never()).save(any(UUID.class), any(MultipartFile.class));
    }

    @Test
    void uploadImages_UTC003_InvalidImageFormat_ShouldThrowValidationException() throws Exception {
        MultipartFile[] newImages = {
                new MockMultipartFile("file1", "document.txt", "text/plain", "test content".getBytes())
        };

        doNothing().when(fileStorageService).createDir(requestId);

        ValidationException exception = assertThrows(ValidationException.class,
                () -> imageService.uploadImages(requestId, currentImages, newImages, request));

        assertEquals("Chỉ hỗ trợ ảnh (PNG, JPG)", exception.getMessage());
        verify(fileStorageService, never()).save(any(UUID.class), any(MultipartFile.class));
    }

    @Test
    void uploadImages_UTC004_MaxImagesExactly_ShouldReturnTotalFiveRequestImages() throws Exception {
        List<RequestImage> existingImages = createRequestImages(3);
        MultipartFile[] newImages = {
                new MockMultipartFile("file1", "image1.jpg", "image/jpeg", "test content".getBytes()),
                new MockMultipartFile("file2", "image2.jpg", "image/jpeg", "test content".getBytes())
        };

        doNothing().when(fileStorageService).createDir(requestId);
        when(fileStorageService.save(eq(requestId), any(MultipartFile.class)))
                .thenReturn("generated_filename_1.jpg")
                .thenReturn("generated_filename_2.jpg");

        List<RequestImage> result = imageService.uploadImages(requestId, existingImages, newImages, request);

        assertEquals(5, result.size());
        verify(fileStorageService, times(2)).save(eq(requestId), any(MultipartFile.class));
    }

    @Test
    void uploadImages_UTC005_IOExceptionDuringSave_ShouldThrowValidationException() throws Exception {
        MultipartFile[] newImages = {
                new MockMultipartFile("file1", "image1.jpg", "image/jpeg", "test content".getBytes())
        };

        doNothing().when(fileStorageService).createDir(requestId);
        when(fileStorageService.save(eq(requestId), any(MultipartFile.class)))
                .thenThrow(new IOException("Disk full"));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> imageService.uploadImages(requestId, currentImages, newImages, request));

        assertTrue(exception.getMessage().contains("Lỗi lưu ảnh:"));
        assertTrue(exception.getMessage().contains("Disk full"));
    }

    @Test
    void deleteImages_UTC001_DeleteExistingImages_ShouldReturnUpdatedList() throws Exception {
        List<RequestImage> images = new ArrayList<>();
        images.add(createRequestImage("imgA.jpg"));
        images.add(createRequestImage("imgB.jpg"));
        String[] toDelete = { "imgA.jpg" };

        doNothing().when(fileStorageService).deleteIfExists(requestId, "imgA.jpg");

        List<RequestImage> result = imageService.deleteImages(requestId, images, toDelete);

        assertEquals(1, result.size());
        assertEquals("imgB.jpg", result.get(0).getFilename());
        verify(fileStorageService).deleteIfExists(requestId, "imgA.jpg");
    }

    @Test
    void deleteImages_UTC002_DeleteMultipleExistingImages_ShouldReturnUpdatedList() throws Exception {
        List<RequestImage> images = new ArrayList<>();
        images.add(createRequestImage("imgA.jpg"));
        images.add(createRequestImage("imgB.jpg"));
        images.add(createRequestImage("imgC.jpg"));
        String[] toDelete = { "imgA.jpg", "imgC.jpg" };

        doNothing().when(fileStorageService).deleteIfExists(eq(requestId), anyString());

        List<RequestImage> result = imageService.deleteImages(requestId, images, toDelete);

        assertEquals(1, result.size());
        assertEquals("imgB.jpg", result.get(0).getFilename());
        verify(fileStorageService).deleteIfExists(requestId, "imgA.jpg");
        verify(fileStorageService).deleteIfExists(requestId, "imgC.jpg");
    }

    @Test
    void deleteImages_UTC003_NoImagesToDelete_ShouldReturnOriginalList() throws Exception {
        List<RequestImage> images = createRequestImages(2);
        String[] toDelete = null;

        List<RequestImage> result = imageService.deleteImages(requestId, images, toDelete);

        assertEquals(images, result);
        verify(fileStorageService, never()).deleteIfExists(any(UUID.class), anyString());
    }

    @Test
    void deleteImages_UTC004_IOExceptionDuringDeletion_ShouldThrowValidationException() throws Exception {
        List<RequestImage> images = new ArrayList<>();
        images.add(createRequestImage("imgA.jpg"));
        String[] toDelete = { "imgA.jpg" };

        doThrow(new IOException("Permission denied"))
                .when(fileStorageService).deleteIfExists(requestId, "imgA.jpg");

        ValidationException exception = assertThrows(ValidationException.class,
                () -> imageService.deleteImages(requestId, images, toDelete));

        assertTrue(exception.getMessage().contains("Lỗi xóa ảnh:"));
        assertTrue(exception.getMessage().contains("Permission denied"));
    }

    @Test
    void deleteImages_UTC005_AttemptToDeleteNonexistentImage_ShouldReturnUnchangedList() throws Exception {
        List<RequestImage> images = new ArrayList<>();
        images.add(createRequestImage("imgA.jpg"));
        String[] toDelete = { "nonExistent.jpg" };

        doNothing().when(fileStorageService).deleteIfExists(requestId, "nonExistent.jpg");

        List<RequestImage> result = imageService.deleteImages(requestId, images, toDelete);

        assertEquals(1, result.size());
        assertEquals("imgA.jpg", result.get(0).getFilename());
        verify(fileStorageService).deleteIfExists(requestId, "nonExistent.jpg");
    }

    private List<RequestImage> createRequestImages(int count) {
        List<RequestImage> images = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            images.add(createRequestImage("image" + i + ".jpg"));
        }
        return images;
    }

    private RequestImage createRequestImage(String filename) {
        RequestImage image = new RequestImage();
        image.setFilename(filename);
        image.setRequest(request);
        return image;
    }
}