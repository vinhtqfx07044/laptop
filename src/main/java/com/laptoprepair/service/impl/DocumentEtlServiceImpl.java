package com.laptoprepair.service.impl;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.lang.NonNull;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.laptoprepair.exception.SystemException;
import com.laptoprepair.service.DocumentEtlService;

/**
 * Implementation of Document ETL service using Spring AI's ETL framework.
 */
@Service
@RequiredArgsConstructor
public class DocumentEtlServiceImpl implements DocumentEtlService {

    private static final Logger log = LoggerFactory.getLogger(DocumentEtlServiceImpl.class);

    private final VectorStore vectorStore;
    private final TokenTextSplitter tokenTextSplitter;

    @Async("documentEtlExecutor")
    @Override
    public CompletableFuture<Integer> processDocument(UUID documentId, @NonNull String filePath) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        log.info("[{}] Starting ETL process for document ID: {} from file: {}", correlationId,
                documentId, filePath);

        try {
            // Validate file exists and is readable
            File file = new File(filePath);
            if (!file.exists()) {
                throw new IllegalArgumentException("File does not exist: " + filePath);
            }
            if (!file.canRead()) {
                throw new IllegalArgumentException("File is not readable: " + filePath);
            }

            // Step 1: Extract text from PDF using Spring AI's PagePdfDocumentReader
            log.info("[{}] Extracting text from PDF file: {}", correlationId, filePath);
            Resource pdfResource = new FileSystemResource(filePath);

            PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder().withPageTopMargin(0)
                    .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                            .withNumberOfTopTextLinesToDelete(0)
                            .withNumberOfBottomTextLinesToDelete(0).build())
                    .withPagesPerDocument(1).build();

            DocumentReader pdfReader = new PagePdfDocumentReader(pdfResource, config);
            List<Document> documents = pdfReader.read();

            log.info("[{}] Extracted {} documents from PDF", correlationId, documents.size());

            if (documents.isEmpty()) {
                log.warn("[{}] No content extracted from PDF file: {}", correlationId, filePath);
                return CompletableFuture.completedFuture(0);
            }

            // Add document metadata to each extracted page
            for (Document doc : documents) {
                Map<String, Object> metadata = doc.getMetadata();
                metadata.put("documentId", documentId.toString());
                metadata.put("sourceFile", filePath);
                metadata.put("correlationId", correlationId);
                metadata.put("processedAt", System.currentTimeMillis());
            }

            // Step 2: Split documents into chunks using TokenTextSplitter
            log.info("[{}] Splitting documents into chunks", correlationId);
            List<Document> splitDocuments = tokenTextSplitter.apply(documents);
            log.info("[{}] Split into {} chunks from {} original documents", correlationId,
                    splitDocuments.size(), documents.size());

            // Step 3: Generate embeddings and store in vector database
            log.info("[{}] Generating embeddings and storing in vector database", correlationId);
            vectorStore.add(splitDocuments);

            long processingDuration = System.currentTimeMillis() - startTime;
            log.info(
                    "[{}] ETL process completed successfully for document ID: {}. Created {} embeddings in {}ms",
                    correlationId, documentId, splitDocuments.size(), processingDuration);

            return CompletableFuture.completedFuture(splitDocuments.size());

        } catch (IllegalArgumentException e) {
            throw new SystemException(
                    String.format("ETL process failed for document %s: %s", documentId, e.getMessage()),
                    "DOCUMENT_PROCESSING_ERROR", e);

        } catch (Exception e) {
            throw new SystemException(
                    String.format("Unexpected error during ETL process for document: %s", documentId),
                    "DOCUMENT_PROCESSING_ERROR", e);
        }
    }

    @Async("documentEtlExecutor")
    @Override
    public CompletableFuture<Boolean> deleteEmbeddings(UUID documentId) {
        String correlationId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        log.info("[{}] Starting deletion of embeddings for document ID: {}", correlationId,
                documentId);

        try {
            // Create filter expression to match documents by documentId metadata
            String filterExpression = "documentId == '" + documentId.toString() + "'";

            // Delete documents matching the filter
            vectorStore.delete(filterExpression);

            long processingDuration = System.currentTimeMillis() - startTime;
            log.info("[{}] Successfully deleted embeddings for document ID: {} in {}ms",
                    correlationId, documentId, processingDuration);

            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            throw new SystemException(
                    String.format("Unexpected error during embedding deletion for document: %s", documentId),
                    "EMBEDDING_DELETION_ERROR", e);
        }
    }
}
