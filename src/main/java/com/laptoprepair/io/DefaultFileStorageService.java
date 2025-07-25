package com.laptoprepair.io;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class DefaultFileStorageService implements FileStorageService {

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void createDir(UUID requestId) throws IOException {
        Path requestDir = Paths.get(uploadDir, requestId.toString());
        if (!Files.exists(requestDir)) {
            Files.createDirectories(requestDir);
        }
    }

    @Override
    public void deleteIfExists(UUID requestId, String filename) throws IOException {
        Path requestDir = Paths.get(uploadDir, requestId.toString());
        Files.deleteIfExists(requestDir.resolve(filename));
    }

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