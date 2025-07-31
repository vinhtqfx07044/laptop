package com.laptoprepair.validation;

import com.laptoprepair.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ImageValidatorTest {

        @InjectMocks
        private ImageValidator imageValidator;

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(imageValidator, "maxImages", 5);
        }

        @Test
        void validateImageFileSizeAndFormat_WithWrongContentType_ShouldThrowValidationException() {
                MultipartFile file = new MockMultipartFile(
                                "image", "test.txt", "text/plain", "test content".getBytes());

                assertThatThrownBy(() -> imageValidator.validateImageFileSizeAndFormat(file))
                                .isInstanceOf(ValidationException.class)
                                .hasMessage("Chỉ hỗ trợ ảnh (PNG, JPG)");
        }

        @Test
        void validateImageFileSizeAndFormat_WithOversizedFile_ShouldThrowValidationException() {
                byte[] largeContent = new byte[25_000_000]; // 25MB
                MultipartFile file = new MockMultipartFile(
                                "image", "test.jpg", "image/jpeg", largeContent);

                assertThatThrownBy(() -> imageValidator.validateImageFileSizeAndFormat(file))
                                .isInstanceOf(ValidationException.class)
                                .hasMessage("Ảnh quá lớn (tối đa 5MB)");
        }

        @Test
        void validateImageFileSizeAndFormat_WithNullContentType_ShouldThrowValidationException() {
                MultipartFile file = new MockMultipartFile(
                                "image", "test.jpg", null, "content".getBytes());

                assertThatThrownBy(() -> imageValidator.validateImageFileSizeAndFormat(file))
                                .isInstanceOf(ValidationException.class)
                                .hasMessage("Chỉ hỗ trợ ảnh (PNG, JPG)");
        }

        @Test
        void validateMaxImagesPerRequest_WithExceedingLimit_ShouldThrowValidationException() {
                List<String> existingImages = new ArrayList<>();
                existingImages.add("image1.jpg");
                existingImages.add("image2.jpg");
                existingImages.add("image3.jpg");

                MultipartFile[] newImages = {
                                new MockMultipartFile("image1", "new1.jpg", "image/jpeg", "content1".getBytes()),
                                new MockMultipartFile("image2", "new2.jpg", "image/jpeg", "content2".getBytes()),
                                new MockMultipartFile("image3", "new3.jpg", "image/jpeg", "content3".getBytes())
                };

                assertThatThrownBy(() -> imageValidator.validateMaxImagesPerRequest(existingImages, newImages))
                                .isInstanceOf(ValidationException.class)
                                .hasMessage("Tối đa 5 ảnh cho mỗi yêu cầu");
        }
}