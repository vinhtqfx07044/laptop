package com.laptoprepair.repository;

import com.laptoprepair.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByDeletedFalse();

    Optional<Document> findByIdAndDeletedFalse(UUID id);

    List<Document> findByCreatedByAndDeletedFalse(String createdBy);

    @Modifying
    @Query("UPDATE Document d SET d.deleted = true, d.deletedAt = CURRENT_TIMESTAMP, d.deletedBy = :deletedBy WHERE d.id = :id")
    int softDeleteById(@Param("id") UUID id, @Param("deletedBy") String deletedBy);

    long countByDeletedFalse();

    long countByProcessingStatusAndDeletedFalse(Document.ProcessingStatus status);
}