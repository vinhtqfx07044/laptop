package com.laptoprepair.service;

import com.laptoprepair.entity.Document;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for document management operations.
 * Handles file upload, storage, validation, and orchestration with ETL
 * processing.
 */
public interface DocumentService {
    Document uploadDocument(String title, String description, MultipartFile file, String createdBy);

    boolean deleteDocument(UUID id, String deletedBy);

    List<Document> getAllDocuments();

    Optional<Document> getDocumentById(UUID id);

    List<Document> getDocumentsByCreatedBy(String createdBy);

    void updateProcessingStatus(UUID id, Document.ProcessingStatus status, String error,
            Integer chunkCount, String embeddingModel);

    void validatePdfFile(MultipartFile file);

    long countActiveDocuments();

    long countByProcessingStatus(Document.ProcessingStatus status);

    List<Document> searchDocuments(String search, Document.ProcessingStatus status);
}