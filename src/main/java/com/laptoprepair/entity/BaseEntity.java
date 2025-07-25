package com.laptoprepair.entity;

import com.laptoprepair.util.TimeZoneUtil;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

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
        createdAt = TimeZoneUtil.nowInVietnam();
        updatedAt = TimeZoneUtil.nowInVietnam();
    }

    @PreUpdate
    protected void preUpdate() {
        updatedAt = TimeZoneUtil.nowInVietnam();
    }
}