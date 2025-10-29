package com.laptoprepair.controller;

import com.laptoprepair.entity.Document;
import com.laptoprepair.entity.Document.ProcessingStatus;
import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.exception.ValidationException;
import com.laptoprepair.service.DocumentService;
import com.laptoprepair.service.SecurityService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Controller for document management operations.
 * Provides endpoints for public document viewing and staff document management.
 */
@Controller
@RequiredArgsConstructor
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);
    private static final String ERROR_MESSAGE = "errorMessage";

    private final DocumentService documentService;
    private final SecurityService securityService;

    @GetMapping("/documents")
    public String listPublicDocuments(Model model) {
        List<Document> documents = documentService.getAllDocuments();
        if (documents == null) {
            documents = Collections.emptyList();
        }
        model.addAttribute("documents", documents);
        return "public/documents";
    }

    @GetMapping("/staff/documents")
    @PreAuthorize("hasRole('STAFF')")
    public String listStaffDocuments(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            Model model) {

        ProcessingStatus statusEnum = null;
        if (status != null && !status.trim().isEmpty()) {
            try {
                statusEnum = ProcessingStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid status parameter: {}", status);
            }
        }

        List<Document> documents = documentService.searchDocuments(search, statusEnum);

        if (documents == null) {
            documents = Collections.emptyList();
        }

        model.addAttribute("documents", documents);
        model.addAttribute("search", search);
        model.addAttribute("status", statusEnum);

        return "staff/document-list";
    }

    @PostMapping("/staff/documents/upload")
    @PreAuthorize("hasRole('STAFF')")
    public String uploadDocument(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam MultipartFile file,
            RedirectAttributes redirectAttributes) {

        log.info("Staff document upload request: {}", file.getOriginalFilename());

        try {
            String createdBy = securityService.getCurrentUsername();
            Document document = documentService.uploadDocument(title, description, file, createdBy);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Tài liệu '" + document.getTitle() + "' đã được tải lên thành công.");

            log.info("Document uploaded successfully: {}", document.getId());

        } catch (ValidationException e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE, "Lỗi hệ thống. Vui lòng thử lại.");
        }

        return "redirect:/staff/documents";
    }

    @PostMapping("/staff/documents/delete/{id}")
    @PreAuthorize("hasRole('STAFF')")
    public String deleteDocument(
            @PathVariable UUID id,
            RedirectAttributes redirectAttributes) {

        log.info("Staff document delete request: {}", id);

        try {
            // Get current user from security context
            String deletedBy = securityService.getCurrentUsername();

            boolean deleted = documentService.deleteDocument(id, deletedBy);

            if (deleted) {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Tài liệu đã được xóa thành công.");
                log.info("Document deleted successfully: {}", id);
            } else {
                redirectAttributes.addFlashAttribute(ERROR_MESSAGE,
                        "Không tìm thấy tài liệu để xóa.");
                log.warn("Document not found for deletion: {}", id);
            }

        } catch (Exception e) {
            log.error("Error during document deletion: {}", id, e);
            redirectAttributes.addFlashAttribute(ERROR_MESSAGE,
                    "Lỗi khi xóa tài liệu. Vui lòng thử lại.");
        }

        return "redirect:/staff/documents";
    }

    @GetMapping("/documents/download/{id}")
    @SuppressWarnings("null")
    public ResponseEntity<Resource> downloadDocument(@PathVariable UUID id) {
        log.debug("Public request to download document: {}", id);

        try {
            Document document = documentService.getDocumentById(id)
                    .orElseThrow(() -> new NotFoundException("Document not found"));

            Path filePath = Paths.get(document.getFilePath() != null ? document.getFilePath() : "");
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(filePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + document.getFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Error downloading document: {}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/staff/documents/download/{id}")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<Resource> downloadStaffDocument(@PathVariable UUID id) {
        log.debug("Staff request to download document: {}", id);
        return downloadDocument(id);
    }
}