package com.laptoprepair.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.laptoprepair.service.FileStorageService;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/**
 * Implementation of the {@link FileStorageService} interface.
 * Handles storing and managing files (specifically images) related to repair requests on the local filesystem.
 */
@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    /**
     * Creates a directory for a specific request if it does not already exist.
     * The directory path is constructed using the base upload directory and the request ID.
     * @param requestId The UUID of the request for which to create the directory.
     * @throws IOException If an I/O error occurs during directory creation.
     */
    @Override
    public void createDir(UUID requestId) throws IOException {
        Path requestDir = Paths.get(uploadDir, requestId.toString());
        if (!Files.exists(requestDir)) {
            Files.createDirectories(requestDir);
        }
    }

    /**
     * Deletes a file associated with a specific request if it exists.
     * @param requestId The UUID of the request to which the file belongs.
     * @param filename The name of the file to delete.
     * @throws IOException If an I/O error occurs during file deletion.
     */
    @Override
    public void deleteIfExists(UUID requestId, String filename) throws IOException {
        Path requestDir = Paths.get(uploadDir, requestId.toString());
        Files.deleteIfExists(requestDir.resolve(filename));
    }

    /**
     * Saves a MultipartFile to the filesystem, associating it with a specific request.
     * Generates a unique filename and stores the file in the request's directory.
     * @param requestId The UUID of the request to associate the file with.
     * @param file The MultipartFile to save.
     * @return The generated filename of the saved file.
     * @throws IOException If an I/O error occurs during file saving.
     */
    @Override
    public String save(UUID requestId, MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        String extension = contentType != null && contentType.equals("image/png") ? ".png" : ".jpg";
        String filename = UUID.randomUUID() + extension;

        Path requestDir = Paths.get(uploadDir, requestId.toString());
        Path imagePath = requestDir.resolve(filename);

        Files.copy(file.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }
}