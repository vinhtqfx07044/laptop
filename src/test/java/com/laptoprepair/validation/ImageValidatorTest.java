package com.laptoprepair.validation;

import com.laptoprepair.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ImageValidatorTest {

    @InjectMocks
    private ImageValidator imageValidator;

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
                .hasMessage("Ảnh quá lớn (tối đa 20MB)");
    }
}