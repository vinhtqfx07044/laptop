package com.laptoprepair.repository;

import com.laptoprepair.entity.ServiceItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Optional;
import java.util.UUID;

@PreAuthorize("hasRole('STAFF')")
public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {

       // Find all items with pagination and optional filters
       @Query("SELECT s FROM ServiceItem s WHERE " +
                     "(:keyword IS NULL OR :keyword = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                     "AND (:activeOnly IS NULL OR s.active = :activeOnly) " +
                     "ORDER BY s.name ASC")
       Page<ServiceItem> findWithFilters(
                     @Param("keyword") String keyword,
                     @Param("activeOnly") Boolean activeOnly,
                     Pageable pageable);

       // Find by name (for uniqueness check)
       Optional<ServiceItem> findByName(String name);

       // Check if name exists (excluding specific id for updates)
       @Query("SELECT COUNT(s) > 0 FROM ServiceItem s WHERE s.name = :name AND s.id <> :id")
       boolean existsByNameAndIdNot(@Param("name") String name, @Param("id") UUID id);

       // Find by ID only if active
       @Query("SELECT s FROM ServiceItem s WHERE s.id = :id AND s.active = true")
       Optional<ServiceItem> findByIdAndActive(@Param("id") UUID id);
}