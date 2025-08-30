package com.laptoprepair.repository;

import com.laptoprepair.entity.Request;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for {@link Request} entities.
 * Provides methods for performing CRUD operations and custom queries related to
 * repair requests.
 */
public interface RequestRepository extends JpaRepository<Request, UUID> {

       /**
        * Finds a Request by its ID with items and images eagerly fetched.
        * Optimized for update operations that need items and images.
        * 
        * @param id The UUID of the request.
        * @return An Optional containing the Request if found, otherwise empty.
        */
       @Query("SELECT r FROM Request r " +
              "LEFT JOIN FETCH r.items " +
              "WHERE r.id = :id")
       Optional<Request> findByIdWithItemsAndImages(@Param("id") UUID id);

       /**
        * Finds a Request by its ID with all collections eagerly fetched.
        * Use when all related data is needed.
        * 
        * @param id The UUID of the request.
        * @return An Optional containing the Request if found, otherwise empty.
        */
       @Query("SELECT r FROM Request r " +
              "LEFT JOIN FETCH r.items " +
              "WHERE r.id = :id")
       Optional<Request> findByIdWithAllDetails(@Param("id") UUID id);

       /**
        * Finds a paginated list of Requests based on search criteria and status.
        * 
        * @param search   Optional search term to filter requests by name, phone,
        *                 serial number, or device brand/model.
        * @param status   Optional status to filter requests.
        * @param pageable Pagination information.
        * @return A Page of Request entities.
        */
       @Query(value = "SELECT * FROM request r " +
                     "WHERE (:search IS NULL OR " +
                     "       LOWER(r.name) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
                     "       LOWER(r.phone) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
                     "       LOWER(r.brand_model) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
                     "       LOWER(r.serial_number) LIKE LOWER(CONCAT('%',:search,'%'))) " +
                     "AND (:status IS NULL OR r.status = CAST(:status AS VARCHAR)) " +
                     "ORDER BY r.appointment_date DESC", countQuery = "SELECT COUNT(*) FROM request r " +
                                   "WHERE (:search IS NULL OR " +
                                   "       LOWER(r.name) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
                                   "       LOWER(r.phone) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
                                   "       LOWER(r.brand_model) LIKE LOWER(CONCAT('%',:search,'%')) OR " +
                                   "       LOWER(r.serial_number) LIKE LOWER(CONCAT('%',:search,'%'))) " +
                                   "AND (:status IS NULL OR r.status = CAST(:status AS VARCHAR))", nativeQuery = true)
       Page<Request> findWithFilters(@Param("search") String search,
                     @Param("status") String status,
                     Pageable pageable);

       /**
        * Finds a list of Requests associated with a given email address.
        * This is used for recovery purposes.
        * 
        * @param email The email address to search for.
        * @return A List of Request entities matching the email.
        */
       @Query("SELECT r FROM Request r WHERE r.email = :email")
       List<Request> findByEmail(@Param("email") String email);
}