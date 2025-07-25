package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestImage;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.service.ImageService;
import com.laptoprepair.validation.ImageValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @Value("${upload.max-images-per-request}")
    private int maxImagesPerRequest;

    private final ImageValidator imageValidator;

    @Override
    public List<RequestImage> deleteImages(UUID requestId, List<RequestImage> currentImages, String[] toDelete)
            throws ValidationException {
        if (toDelete == null || toDelete.length == 0) {
            return currentImages;
        }

        Path requestDir = Paths.get(uploadDir, requestId.toString());
        List<RequestImage> updatedImages = new ArrayList<>(currentImages);

        for (String filename : toDelete) {
            try {
                Files.deleteIfExists(requestDir.resolve(filename));
            } catch (IOException e) {
                throw new ValidationException("Lỗi xóa ảnh: " + e.getMessage());
            }
            updatedImages.removeIf(img -> img.getFilename().equals(filename));
        }

        return updatedImages;
    }

    @Override
    public List<RequestImage> uploadImages(UUID requestId, List<RequestImage> currentImages, MultipartFile[] newImages, Request request)
            throws ValidationException {

        if (newImages == null || newImages.length == 0) {
            return currentImages;
        }

        // Convert to filenames for validation
        List<String> currentFilenames = currentImages.stream()
                .map(RequestImage::getFilename)
                .toList();
        imageValidator.validateMaxImagesPerRequest(currentFilenames, newImages);

        Path requestDir = Paths.get(uploadDir, requestId.toString());
        try {
            if (!Files.exists(requestDir)) {
                Files.createDirectories(requestDir);
            }
        } catch (IOException e) {
            throw new ValidationException("Lỗi tạo thư mục lưu ảnh: " + e.getMessage());
        }

        List<RequestImage> updatedImages = new ArrayList<>(currentImages);

        for (MultipartFile file : newImages) {
            if (!file.isEmpty()) {
                imageValidator.validateImageFileSizeAndFormat(file);
                String filename = uploadImage(requestDir, file);
                
                RequestImage requestImage = new RequestImage();
                requestImage.setFilename(filename);
                requestImage.setRequest(request);
                updatedImages.add(requestImage);
            }
        }

        return updatedImages;
    }

    @Override
    public List<RequestImage> updateRequestServiceImages(Request request, MultipartFile[] newImages, String[] toDelete)
            throws ValidationException {
        UUID requestId = request.getId();
        if (requestId == null) {
            throw new IllegalStateException("Không tìm thấy phiếu sửa chữa.");
        }

        List<RequestImage> currentImages = new ArrayList<>(request.getImages());
        currentImages = deleteImages(requestId, currentImages, toDelete);
        currentImages = uploadImages(requestId, currentImages, newImages, request);

        return currentImages;
    }

    private String uploadImage(Path requestDir, MultipartFile file) throws ValidationException {
        String contentType = file.getContentType();
        String extension = contentType != null && contentType.equals("image/png") ? ".png" : ".jpg";
        String filename = UUID.randomUUID() + extension;
        Path imagePath = requestDir.resolve(filename);

        try {
            Files.copy(file.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException e) {
            throw new ValidationException("Lỗi lưu ảnh: " + e.getMessage());
        }
    }
}