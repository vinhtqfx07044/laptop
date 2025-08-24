package com.laptoprepair.service.impl;

import com.laptoprepair.entity.Request;
import com.laptoprepair.entity.RequestImage;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.service.FileStorageService;
import com.laptoprepair.service.ImageService;
import com.laptoprepair.validation.ImageValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of the {@link ImageService} interface.
 * Handles the business logic for managing images associated with repair
 * requests,
 * including deletion, uploading, and updating.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    @Value("${app.upload.max-images-per-request}")
    private int maxImagesPerRequest;

    private final ImageValidator imageValidator;
    private final FileStorageService fileStorageService;

    /**
     * Deletes specified images associated with a request.
     * 
     * @param requestId     The ID of the request.
     * @param currentImages The current list of images for the request.
     * @param toDelete      An array of filenames to be deleted.
     * @return The updated list of images after deletion.
     * @throws ValidationException If an error occurs during image deletion.
     */
    @Override
    public List<RequestImage> deleteImages(UUID requestId, List<RequestImage> currentImages, String[] toDelete)
            throws ValidationException {
        if (toDelete == null || toDelete.length == 0) {
            return currentImages;
        }

        List<RequestImage> updatedImages = new ArrayList<>(currentImages);

        for (String filename : toDelete) {
            try {
                fileStorageService.deleteIfExists(requestId, filename);
            } catch (IOException e) {
                throw new ValidationException("Lỗi xóa ảnh: " + e.getMessage());
            }
            updatedImages.removeIf(img -> img.getFilename().equals(filename));
        }

        return updatedImages;
    }

    /**
     * Uploads new images and associates them with a request.
     * 
     * @param requestId     The ID of the request.
     * @param currentImages The current list of images for the request.
     * @param newImages     An array of MultipartFile objects representing the new
     *                      images to upload.
     * @param request       The Request entity to associate the images with.
     * @return The updated list of images after uploading.
     * @throws ValidationException If an error occurs during image upload or
     *                             validation.
     */
    @Override
    public List<RequestImage> uploadImages(UUID requestId, List<RequestImage> currentImages, MultipartFile[] newImages,
            Request request)
            throws ValidationException {
        if (newImages == null || newImages.length == 0) {
            return currentImages;
        }

        // Convert to filenames for validation
        List<String> currentFilenames = currentImages.stream()
                .map(RequestImage::getFilename)
                .toList();
        imageValidator.validateMaxImagesPerRequest(currentFilenames, newImages);

        try {
            fileStorageService.createDir(requestId);
        } catch (IOException e) {
            throw new ValidationException("Lỗi tạo thư mục lưu ảnh: " + e.getMessage());
        }

        List<RequestImage> updatedImages = new ArrayList<>(currentImages);

        for (MultipartFile file : newImages) {
            if (!file.isEmpty()) {
                imageValidator.validateImageFileSizeAndFormat(file);
                try {
                    String filename = fileStorageService.save(requestId, file);
                    RequestImage requestImage = new RequestImage();
                    requestImage.setFilename(filename);
                    requestImage.setRequest(request);
                    updatedImages.add(requestImage);
                } catch (IOException e) {
                    throw new ValidationException("Lỗi lưu ảnh: " + e.getMessage());
                }
            }
        }

        return updatedImages;
    }

    /**
     * Updates the images associated with a request by deleting old ones and
     * uploading new ones.
     * 
     * @param request   The Request entity whose images are to be updated.
     * @param newImages An array of MultipartFile objects representing new images to
     *                  add.
     * @param toDelete  An array of filenames of images to be deleted.
     * @return The final list of RequestImage objects after updates.
     * @throws ValidationException If an error occurs during image processing.
     */
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

}