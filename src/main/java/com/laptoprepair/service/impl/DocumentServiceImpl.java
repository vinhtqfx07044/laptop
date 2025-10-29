package com.laptoprepair.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.laptoprepair.entity.Document;
import com.laptoprepair.entity.Document.ProcessingStatus;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.exception.SystemException;
import com.laptoprepair.repository.DocumentRepository;
import com.laptoprepair.service.DocumentEtlService;
import com.laptoprepair.service.DocumentService;

import jakarta.annotation.PostConstruct;

/**
 * Implementation of DocumentService for document management operations.
 * Handles file validation, storage, and coordination with ETL processing.
 */
@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentServiceImpl.class);

    private final DocumentRepository documentRepository;
    private final DocumentEtlService documentEtlService;

    private static final String UPLOAD_DIRECTORY = "./documents";

    // Fixed maximum file size: 50MB
    private static final long MAX_FILE_SIZE_BYTES = 52428800L; // 50 * 1024 * 1024

    @Value("${spring.ai.openai.embedding.model}")
    private String embeddingModel;

    public DocumentServiceImpl(DocumentRepository documentRepository, DocumentEtlService documentEtlService) {
        this.documentRepository = documentRepository;
        this.documentEtlService = documentEtlService;
    }

    /**
     * Initialize service after dependency injection is complete
     */
    @PostConstruct
    public void initialize() {
        createUploadDirectoryIfNotExists();
    }

    @Override
    public Document uploadDocument(String title, String description, MultipartFile file, String createdBy) {
        log.info("Uploading document: {} by user: {}", file.getOriginalFilename(), createdBy);

        try {
            // Validate file
            validatePdfFile(file);

            // Create document entity
            Document document = new Document();
            document.setTitle(title);
            document.setDescription(description);
            document.setFileName(file.getOriginalFilename());
            document.setFileExtension("pdf");
            document.setFileSizeBytes(file.getSize());
            document.setProcessingStatus(Document.ProcessingStatus.PENDING);

            // Save to storage
            String filePath = saveFileToStorage(file);
            document.setFilePath(filePath);

            // Save to database
            Document savedDocument = documentRepository.save(document);
            log.info("Document saved successfully: {}", savedDocument.getId());

            // Trigger async ETL processing
            documentEtlService.processDocument(savedDocument.getId(), filePath != null ? filePath : "")
                    .thenAccept(chunkCount -> {
                        log.info("ETL processing completed for document {}: {} chunks", savedDocument.getId(),
                                chunkCount);
                        updateProcessingStatus(savedDocument.getId(), Document.ProcessingStatus.COMPLETED, null,
                                chunkCount, embeddingModel);
                    })
                    .exceptionally(throwable -> {
                        log.error("ETL processing failed for document {}", savedDocument.getId(), throwable);
                        updateProcessingStatus(savedDocument.getId(), Document.ProcessingStatus.FAILED,
                                throwable.getMessage(), null, null);
                        return null;
                    });

            return savedDocument;

        } catch (ValidationException | SystemException e) {
            throw e;
        } catch (Exception e) {
            throw new SystemException("Lỗi xử lý tài liệu: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean deleteDocument(UUID id, String deletedBy) {
        log.info("Deleting document: {} by user: {}", id, deletedBy);

        try {
            Optional<Document> documentOpt = documentRepository.findByIdAndDeletedFalse(id);
            if (documentOpt.isEmpty()) {
                log.warn("Document not found: {}", id);
                return false;
            }

            Document document = documentOpt.get();

            // Delete physical file
            deletePhysicalFile(document.getFilePath());

            // Delete embeddings from vector store
            documentEtlService.deleteEmbeddings(id)
                    .thenAccept(success -> {
                        if (Boolean.TRUE.equals(success)) {
                            log.info("Embeddings deleted successfully for document: {}", id);
                        } else {
                            log.warn("Failed to delete embeddings for document: {}", id);
                        }
                    })
                    .exceptionally(throwable -> {
                        log.error("Error deleting embeddings for document: {}", id, throwable);
                        return null;
                    });

            // Soft delete from database
            int updatedRows = documentRepository.softDeleteById(id, deletedBy);
            if (updatedRows > 0) {
                log.info("Document soft deleted successfully: {}", id);
                return true;
            } else {
                log.warn("Failed to soft delete document: {}", id);
                return false;
            }

        } catch (Exception e) {
            throw new SystemException("Lỗi xóa tài liệu: " + e.getMessage());
        }
    }

    @Override
    public List<Document> getAllDocuments() {
        log.debug("Retrieving all active documents");
        return documentRepository.findByDeletedFalse();
    }

    @Override
    public Optional<Document> getDocumentById(UUID id) {
        log.debug("Retrieving document by id: {}", id);
        return documentRepository.findByIdAndDeletedFalse(id);
    }

    @Override
    public List<Document> getDocumentsByCreatedBy(String createdBy) {
        log.debug("Retrieving documents uploaded by: {}", createdBy);
        return documentRepository.findByCreatedByAndDeletedFalse(createdBy);
    }

    @Override
    public void updateProcessingStatus(UUID id, Document.ProcessingStatus status, String error,
            Integer chunkCount, String embeddingModel) {
        log.debug("Updating processing status for document {}: {} - {}", id, status, error);

        if (id == null) {
            log.warn("Cannot update processing status for null document id");
            return;
        }

        try {
            Optional<Document> documentOpt = documentRepository.findById(id);
            if (documentOpt.isPresent()) {
                Document document = documentOpt.get();
                document.setProcessingStatus(status);
                document.setProcessingError(error);
                document.setEmbeddingChunkCount(chunkCount);
                document.setEmbeddingModel(embeddingModel);
                documentRepository.save(document);
                log.info("Processing status updated for document: {}", id);
            } else {
                log.warn("Document not found for status update: {}", id);
            }
        } catch (Exception e) {
            log.error("Error updating processing status for document: {} - Status: {}, Error: {}", id, status, error,
                    e);
            // Don't throw exception in async context to avoid breaking the processing flow
        }
    }

    @Override
    public void validatePdfFile(MultipartFile file) {
        log.debug("Validating PDF file: {}", file.getOriginalFilename());

        // Check if file is empty
        if (file.isEmpty()) {
            throw new ValidationException("File rỗng");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ValidationException("File vượt quá 50MB");
        }

        // Check filename
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty() || filename.length() > 255) {
            throw new ValidationException("Tên file không hợp lệ");
        }

        // Check filename contains only safe characters
        if (!filename.matches("^[a-zA-Z0-9._-]+$")) {
            throw new ValidationException("Tên file không hợp lệ");
        }

        // Check file extension
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        List<String> allowedExtList = Arrays.asList("pdf");
        if (!allowedExtList.contains(extension)) {
            throw new ValidationException("Chỉ chấp nhận file PDF");
        }

        // Check MIME type
        String contentType = file.getContentType();
        List<String> allowedMimeList = Arrays.asList("application/pdf");
        if (contentType == null || !allowedMimeList.contains(contentType)) {
            throw new ValidationException("Chỉ chấp nhận file PDF");
        }

        // Check if PDF is readable and not encrypted using Apache PDFBox
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            AccessPermission accessPermission = document.getCurrentAccessPermission();

            // Check if PDF is encrypted and content cannot be extracted
            if (document.isEncrypted() && !accessPermission.canExtractContent()) {
                throw new ValidationException("File PDF bị mã hóa");
            }

            // Check if PDF has content
            if (document.getNumberOfPages() <= 0) {
                throw new ValidationException("File PDF bị lỗi");
            }

            log.debug("PDF validation successful: {} pages, encrypted: {}",
                    document.getNumberOfPages(), document.isEncrypted());

        } catch (ValidationException e) {
            // Rethrow our own validation exceptions
            throw e;
        } catch (IOException e) {
            log.warn("PDF validation failed - IOException: {}", e.getMessage());
            throw new ValidationException("File PDF bị lỗi");
        } catch (Exception e) {
            log.warn("PDF validation failed: {}", e.getMessage());
            throw new ValidationException("File PDF bị lỗi");
        }
    }

    @Override
    public long countActiveDocuments() {
        return documentRepository.countByDeletedFalse();
    }

    @Override
    public long countByProcessingStatus(Document.ProcessingStatus status) {
        return documentRepository.countByProcessingStatusAndDeletedFalse(status);
    }

    @Override
    public List<Document> searchDocuments(String search, ProcessingStatus status) {
        log.debug("Searching documents with search: {}, status: {}", search, status);

        // If no filters, return all active documents
        if ((search == null || search.trim().isEmpty()) && status == null) {
            return documentRepository.findByDeletedFalse();
        }

        // Implement search logic
        List<Document> results = documentRepository.findByDeletedFalse();

        // Filter by search term (title contains search term)
        if (search != null && !search.trim().isEmpty()) {
            String searchTerm = search.trim().toLowerCase();
            results = results.stream()
                    .filter(doc -> (doc.getTitle() != null && doc.getTitle().toLowerCase().contains(searchTerm)))
                    .toList();
        }

        // Filter by status
        if (status != null) {
            results = results.stream()
                    .filter(doc -> status.equals(doc.getProcessingStatus()))
                    .toList();
        }

        log.debug("Found {} documents matching filters", results.size());
        return results;
    }

    /**
     * Save uploaded file to storage directory
     */
    private String saveFileToStorage(MultipartFile file) throws SystemException {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                throw new ValidationException("Tên file không hợp lệ");
            }
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String uniqueFilename = UUID.randomUUID().toString() + extension;

            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            if (log.isInfoEnabled()) {
                log.info("File saved to: {}", filePath);
            }
            return filePath.toString();

        } catch (IOException e) {
            throw new SystemException("Lỗi lưu trữ file", "ERR-DOC-STORAGE", e);
        }
    }

    /**
     * Delete physical file from storage
     */
    private void deletePhysicalFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("Physical file deleted: {}", filePath);
            } else {
                log.warn("Physical file not found for deletion: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete physical file: {}", filePath, e);
            // Continue with database deletion even if file deletion fails
        }
    }

    /**
     * Create upload directory if it doesn't exist
     * Protected to allow overriding in tests
     */
    protected void createUploadDirectoryIfNotExists() {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIRECTORY);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", UPLOAD_DIRECTORY);
            }
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", UPLOAD_DIRECTORY, e);
        }
    }
}