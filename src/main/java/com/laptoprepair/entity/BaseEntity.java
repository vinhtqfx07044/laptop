package com.laptoprepair.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

import com.laptoprepair.utils.DefaultTimeProvider;

/**
 * Base entity class providing common fields and auditing capabilities.
 * All other entities should extend this class.
 */
@MappedSuperclass
@Data
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    /**
     * Automatically sets the creation and update timestamps before persisting the entity.
     */
    @PrePersist
    protected void prePersist() {
        createdAt = DefaultTimeProvider.nowInVietnam();
        updatedAt = DefaultTimeProvider.nowInVietnam();
    }

    /**
     * Automatically updates the update timestamp before updating the entity.
     */
    @PreUpdate
    protected void preUpdate() {
        updatedAt = DefaultTimeProvider.nowInVietnam();
    }
}