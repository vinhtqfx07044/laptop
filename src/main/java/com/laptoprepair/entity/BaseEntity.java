package com.laptoprepair.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

import com.laptoprepair.utils.DefaultTimeProvider;
import com.laptoprepair.utils.SpringContext;

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

    @PrePersist
    protected void prePersist() {
        DefaultTimeProvider timeProvider = SpringContext.getBean(DefaultTimeProvider.class);
        createdAt = timeProvider.now();
        updatedAt = timeProvider.now();
    }

    @PreUpdate
    protected void preUpdate() {
        DefaultTimeProvider timeProvider = SpringContext.getBean(DefaultTimeProvider.class);
        updatedAt = timeProvider.now();
    }
}