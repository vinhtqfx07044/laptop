package com.laptoprepair.validation;

import com.laptoprepair.exception.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Validator for image-related operations, specifically for request images.
 * Ensures images adhere to size and format constraints and limits the number of
 * images per request.
 */
@Component
public class ImageValidator {
    @Value("${app.upload.max-images-per-request}")
    private int maxImages;

    public void validateMaxImagesPerRequest(List<String> existingImages, MultipartFile[] newImages) {
        int count = existingImages.size();

        if (newImages == null || newImages.length == 0) {
            return;
        }

        for (MultipartFile f : newImages) {
            if (!f.isEmpty()) {
                count++;
            }
        }

        if (count > maxImages) {
            throw new ValidationException("Tối đa " + maxImages + " ảnh cho mỗi yêu cầu");
        }
    }

    public void validateImageFileSizeAndFormat(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new ValidationException("Chỉ hỗ trợ ảnh (PNG, JPG)");
        }

        if (file.getSize() > 5_000_000) {
            throw new ValidationException("Ảnh quá lớn (tối đa 5MB)");
        }
    }
}