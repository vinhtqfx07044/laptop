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

/**
 * Repository interface for {@link ServiceItem} entities.
 * Provides methods for performing CRUD operations and custom queries related to
 * service items.
 */
@PreAuthorize("hasRole('STAFF')")
public interface ServiceItemRepository extends JpaRepository<ServiceItem, UUID> {

       /**
        * Finds a paginated list of ServiceItems based on a keyword and active status.
        * 
        * @param keyword    Optional keyword to filter service items by name.
        * @param activeOnly Optional boolean to filter for active service items only.
        * @param pageable   Pagination information.
        * @return A Page of ServiceItem entities.
        */
       @Query(value = "SELECT * FROM service_item s WHERE " +
                     "(:keyword IS NULL OR :keyword = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
                     "AND (:activeOnly IS NULL OR s.active = :activeOnly) " +
                     "ORDER BY s.name ASC", countQuery = "SELECT COUNT(*) FROM service_item s WHERE " +
                                   "(:keyword IS NULL OR :keyword = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
                                   +
                                   "AND (:activeOnly IS NULL OR s.active = :activeOnly)", nativeQuery = true)
       Page<ServiceItem> findWithFilters(
                     @Param("keyword") String keyword,
                     @Param("activeOnly") Boolean activeOnly,
                     Pageable pageable);

       /**
        * Finds a ServiceItem by its name.
        * 
        * @param name The name of the service item.
        * @return An Optional containing the ServiceItem if found, otherwise empty.
        */
       Optional<ServiceItem> findByName(String name);

       /**
        * Checks if a service item with the given name already exists, excluding a
        * specific ID.
        * This is used for uniqueness checks during updates.
        * 
        * @param name The name to check.
        * @param id   The ID of the service item to exclude from the check.
        * @return true if a service item with the name exists and its ID is not the
        *         excluded ID, false otherwise.
        */
       @Query("SELECT COUNT(s) > 0 FROM ServiceItem s WHERE s.name = :name AND s.id <> :id")
       boolean existsByNameAndIdNot(@Param("name") String name, @Param("id") UUID id);

       /**
        * Finds a ServiceItem by its ID only if it is active.
        * 
        * @param id The UUID of the service item.
        * @return An Optional containing the active ServiceItem if found, otherwise
        *         empty.
        */
       @Query("SELECT s FROM ServiceItem s WHERE s.id = :id AND s.active = true")
       Optional<ServiceItem> findByIdAndActive(@Param("id") UUID id);
}