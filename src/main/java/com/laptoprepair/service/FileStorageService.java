package com.laptoprepair.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Service interface for managing file storage operations, specifically for
 * request-related files.
 */
public interface FileStorageService {
    void createDir(UUID requestId) throws IOException;

    void deleteIfExists(UUID requestId, String filename) throws IOException;

    String save(UUID requestId, MultipartFile file) throws IOException;
}