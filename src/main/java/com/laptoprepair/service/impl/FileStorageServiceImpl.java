package com.laptoprepair.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.laptoprepair.service.FileStorageService;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private static final String UPLOAD_DIR = "images";

    @Override
    public void createDir(UUID requestId) throws IOException {
        Path requestDir = Paths.get(UPLOAD_DIR, requestId.toString());
        if (!Files.exists(requestDir)) {
            Files.createDirectories(requestDir);
        }
    }

    @Override
    public void deleteIfExists(UUID requestId, String filename) throws IOException {
        Path requestDir = Paths.get(UPLOAD_DIR, requestId.toString());
        Files.deleteIfExists(requestDir.resolve(filename));
    }

    @Override
    public String save(UUID requestId, MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        String extension = contentType != null && contentType.equals("image/png") ? ".png" : ".jpg";
        String filename = UUID.randomUUID() + extension;

        Path requestDir = Paths.get(UPLOAD_DIR, requestId.toString());
        Path imagePath = requestDir.resolve(filename);

        Files.copy(file.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }
}