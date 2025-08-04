package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestImage;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.service.FileStorageService;
import com.laptoprepair.validation.ImageValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImageServiceImplTest {

    @Mock
    private ImageValidator imageValidator;
    
    @Mock
    private FileStorageService fileStorageService;
    
    @Mock
    private MultipartFile multipartFile1;
    
    @Mock
    private MultipartFile multipartFile2;
    
    @Mock
    private MultipartFile multipartFile3;
    
    @Mock
    private MultipartFile multipartFile4;
    
    @Mock
    private MultipartFile multipartFile5;

    @InjectMocks
    private ImageServiceImpl imageService;

    private Request testRequest;
    private UUID requestId;
    private List<RequestImage> existingImages;

    @BeforeEach
    void setUp() {
        // Set the max images per request value using reflection
        ReflectionTestUtils.setField(imageService, "maxImagesPerRequest", 5);
        
        requestId = UUID.randomUUID();
        testRequest = new Request();
        testRequest.setId(requestId);
        
        // Create existing images
        existingImages = new ArrayList<>();
        RequestImage image1 = new RequestImage();
        image1.setFilename("existing1.jpg");
        image1.setRequest(testRequest);
        
        RequestImage image2 = new RequestImage();
        image2.setFilename("existing2.jpg");
        image2.setRequest(testRequest);
        
        existingImages.add(image1);
        existingImages.add(image2);
        
        testRequest.setImages(existingImages);
    }

    @DisplayName("imageUpdateTest - UTCID01: Delete one image and add one new image successfully")
    @Test
    void updateRequestServiceImages_DeleteOneAddOne_ShouldReturnUpdatedList() throws IOException {
        // Arrange
        String[] toDelete = {"existing1.jpg"};
        MultipartFile[] newImages = {multipartFile1};
        
        when(multipartFile1.isEmpty()).thenReturn(false);
        when(fileStorageService.save(requestId, multipartFile1)).thenReturn("new1.jpg");
        
        doNothing().when(fileStorageService).deleteIfExists(requestId, "existing1.jpg");
        doNothing().when(fileStorageService).createDir(requestId);
        doNothing().when(imageValidator).validateMaxImagesPerRequest(anyList(), any(MultipartFile[].class));
        doNothing().when(imageValidator).validateImageFileSizeAndFormat(multipartFile1);

        // Act
        List<RequestImage> result = imageService.updateRequestServiceImages(testRequest, newImages, toDelete);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.stream().map(RequestImage::getFilename))
            .containsExactlyInAnyOrder("existing2.jpg", "new1.jpg");
        
        verify(fileStorageService).deleteIfExists(requestId, "existing1.jpg");
        verify(fileStorageService).save(requestId, multipartFile1);
        verify(imageValidator).validateMaxImagesPerRequest(anyList(), eq(newImages));
        verify(imageValidator).validateImageFileSizeAndFormat(multipartFile1);
    }

    @DisplayName("imageUpdateTest - UTCID02: Adding too many images should throw ValidationException")
    @Test
    void updateRequestServiceImages_ExceedMaxImages_ShouldThrowValidationException() {
        // Arrange
        MultipartFile[] newImages = {multipartFile1, multipartFile2, multipartFile3, multipartFile4};
        String expectedMessage = "Tối đa 5 ảnh cho mỗi phiếu";
        
        doThrow(new ValidationException(expectedMessage))
            .when(imageValidator).validateMaxImagesPerRequest(anyList(), eq(newImages));

        // Act & Assert
        assertThatThrownBy(() -> imageService.updateRequestServiceImages(testRequest, newImages, null))
            .isInstanceOf(ValidationException.class)
            .hasMessage(expectedMessage);
        
        verify(imageValidator).validateMaxImagesPerRequest(anyList(), eq(newImages));
        verifyNoInteractions(fileStorageService);
    }

    @DisplayName("imageUpdateTest - UTCID03: Delete all existing and add maximum new images")
    @Test
    void updateRequestServiceImages_DeleteAllAddMaximum_ShouldReturnCorrectCount() throws IOException {
        // Arrange
        String[] toDelete = {"existing1.jpg", "existing2.jpg"};
        MultipartFile[] newImages = {multipartFile1, multipartFile2, multipartFile3, multipartFile4, multipartFile5};
        
        when(multipartFile1.isEmpty()).thenReturn(false);
        when(multipartFile2.isEmpty()).thenReturn(false);
        when(multipartFile3.isEmpty()).thenReturn(false);
        when(multipartFile4.isEmpty()).thenReturn(false);
        when(multipartFile5.isEmpty()).thenReturn(false);
        
        when(fileStorageService.save(requestId, multipartFile1)).thenReturn("new1.jpg");
        when(fileStorageService.save(requestId, multipartFile2)).thenReturn("new2.jpg");
        when(fileStorageService.save(requestId, multipartFile3)).thenReturn("new3.jpg");
        when(fileStorageService.save(requestId, multipartFile4)).thenReturn("new4.jpg");
        when(fileStorageService.save(requestId, multipartFile5)).thenReturn("new5.jpg");
        
        doNothing().when(fileStorageService).deleteIfExists(eq(requestId), anyString());
        doNothing().when(fileStorageService).createDir(requestId);
        doNothing().when(imageValidator).validateMaxImagesPerRequest(anyList(), eq(newImages));
        doNothing().when(imageValidator).validateImageFileSizeAndFormat(any(MultipartFile.class));

        // Act
        List<RequestImage> result = imageService.updateRequestServiceImages(testRequest, newImages, toDelete);

        // Assert
        assertThat(result).hasSize(5);
        assertThat(result.stream().map(RequestImage::getFilename))
            .containsExactlyInAnyOrder("new1.jpg", "new2.jpg", "new3.jpg", "new4.jpg", "new5.jpg");
        
        verify(fileStorageService, times(2)).deleteIfExists(eq(requestId), anyString());
        verify(fileStorageService, times(5)).save(eq(requestId), any(MultipartFile.class));
        verify(imageValidator).validateMaxImagesPerRequest(anyList(), eq(newImages));
        verify(imageValidator, times(5)).validateImageFileSizeAndFormat(any(MultipartFile.class));
    }
}