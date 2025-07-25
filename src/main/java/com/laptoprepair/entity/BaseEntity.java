package com.laptoprepair.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

import com.laptoprepair.utils.DefaultTimeProvider;

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
        createdAt = DefaultTimeProvider.nowInVietnam();
        updatedAt = DefaultTimeProvider.nowInVietnam();
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = DefaultTimeProvider.nowInVietnam();
    }
}