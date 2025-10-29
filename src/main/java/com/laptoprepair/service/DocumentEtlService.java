package com.laptoprepair.service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.springframework.lang.NonNull;

/**
 * Service interface for asynchronous Document ETL (Extract, Transform, Load)
 * operations.
 * Handles PDF document processing, text extraction, embedding generation, and
 * vector storage.
 */
public interface DocumentEtlService {

    CompletableFuture<Integer> processDocument(UUID documentId, @NonNull String filePath);

    CompletableFuture<Boolean> deleteEmbeddings(UUID documentId);
}