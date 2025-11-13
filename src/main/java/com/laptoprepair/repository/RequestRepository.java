package com.laptoprepair.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.laptoprepair.entity.Request;

public interface RequestRepository extends JpaRepository<Request, UUID> {

    /**
     * Finds a Request by its ID with items eagerly fetched. Optimized
     * for update operations that need items.
     *
     * @param id The UUID of the request.
     * @return An Optional containing the Request if found, otherwise empty.
     */
    @Query("SELECT r FROM Request r " + "LEFT JOIN FETCH r.items " + "WHERE r.id = :id")
    Optional<Request> findByIdWithItems(@Param("id") UUID id);

    /**
     * Finds a Request by its ID with images eagerly fetched. Optimized
     * for edit operations that need to display existing images.
     *
     * @param id The UUID of the request.
     * @return An Optional containing the Request if found, otherwise empty.
     */
    @Query("SELECT r FROM Request r " + "LEFT JOIN FETCH r.images " + "WHERE r.id = :id")
    Optional<Request> findByIdWithImages(@Param("id") UUID id);

    /**
     * Finds a paginated list of Requests based on search criteria and status.
     * 
     * @param search   Optional search term to filter requests by name, phone,
     *                 serial number, or
     *                 device brand/model.
     * @param status   Optional status to filter requests.
     * @param pageable Pagination information.
     * @return A Page of Request entities.
     */
    @Query(value = "SELECT * FROM request r " + "WHERE (:search IS NULL OR "
            + "       LOWER(r.name) LIKE LOWER(CONCAT('%',:search,'%')) OR "
            + "       LOWER(r.phone) LIKE LOWER(CONCAT('%',:search,'%')) OR "
            + "       LOWER(r.brand_model) LIKE LOWER(CONCAT('%',:search,'%')) OR "
            + "       LOWER(r.serial_number) LIKE LOWER(CONCAT('%',:search,'%'))) "
            + "AND (:status IS NULL OR r.status = CAST(:status AS VARCHAR)) "
            + "ORDER BY r.appointment_date DESC", nativeQuery = true)
    Page<Request> findWithFilters(@Param("search") String search, @Param("status") String status,
            Pageable pageable);

    /**
     * Finds a list of Requests associated with a given email address. This is used
     * for recovery
     * purposes.
     *
     * @param email The email address to search for.
     * @return A List of Request entities matching the email.
     */
    @Query("SELECT r FROM Request r WHERE r.email = :email")
    List<Request> findByEmail(@Param("email") String email);

    /**
     * Fuzzy search for Requests with PostgreSQL pg_trgm + unaccent extensions.
     */
    @Query(value = """
            SELECT r.* FROM request r
            WHERE
                (:requestId IS NULL OR :requestId = '' OR r.id = CAST(:requestId AS UUID))
                AND (:customerName IS NULL OR :customerName = '' OR similarity(immutable_unaccent(LOWER(r.name)), immutable_unaccent(LOWER(:customerName))) > 0)
                AND (:customerPhone IS NULL OR :customerPhone = '' OR similarity(r.phone, :customerPhone) > 0)
                AND (:customerEmail IS NULL OR :customerEmail = '' OR similarity(immutable_unaccent(LOWER(r.email)), immutable_unaccent(LOWER(:customerEmail))) > 0)
                AND (:customerAddress IS NULL OR :customerAddress = '' OR similarity(immutable_unaccent(LOWER(r.address)), immutable_unaccent(LOWER(:customerAddress))) > 0)
                AND (:brandModel IS NULL OR :brandModel = '' OR similarity(immutable_unaccent(LOWER(r.brand_model)), immutable_unaccent(LOWER(:brandModel))) > 0)
                AND (:serialNumber IS NULL OR :serialNumber = '' OR similarity(immutable_unaccent(LOWER(r.serial_number)), immutable_unaccent(LOWER(:serialNumber))) > 0)
                AND (:description IS NULL OR :description = '' OR similarity(immutable_unaccent(LOWER(r.description)), immutable_unaccent(LOWER(:description))) > 0)
                AND (:status IS NULL OR :status = '' OR r.status = CAST(:status AS VARCHAR))
                AND (:appointmentDateFrom IS NULL OR :appointmentDateFrom = '' OR r.appointment_date >= CAST(:appointmentDateFrom AS TIMESTAMP))
                AND (:appointmentDateTo IS NULL OR :appointmentDateTo = '' OR r.appointment_date <= CAST(:appointmentDateTo AS TIMESTAMP))
                AND (:completedDateFrom IS NULL OR :completedDateFrom = '' OR r.completed_at >= CAST(:completedDateFrom AS TIMESTAMP))
                AND (:completedDateTo IS NULL OR :completedDateTo = '' OR r.completed_at <= CAST(:completedDateTo AS TIMESTAMP))
            ORDER BY
                CASE WHEN :requestId IS NOT NULL AND :requestId != '' THEN 1.0
                     ELSE GREATEST(
                         COALESCE(similarity(immutable_unaccent(LOWER(r.name)), immutable_unaccent(LOWER(:customerName))), 0.0),
                         COALESCE(similarity(r.phone, :customerPhone), 0.0),
                         COALESCE(similarity(immutable_unaccent(LOWER(r.email)), immutable_unaccent(LOWER(:customerEmail))), 0.0),
                         COALESCE(similarity(immutable_unaccent(LOWER(r.address)), immutable_unaccent(LOWER(:customerAddress))), 0.0),
                         COALESCE(similarity(immutable_unaccent(LOWER(r.brand_model)), immutable_unaccent(LOWER(:brandModel))), 0.0),
                         COALESCE(similarity(immutable_unaccent(LOWER(r.serial_number)), immutable_unaccent(LOWER(:serialNumber))), 0.0),
                         COALESCE(similarity(immutable_unaccent(LOWER(r.description)), immutable_unaccent(LOWER(:description))), 0.0)
                     )
                END DESC,
                r.appointment_date DESC
            LIMIT 10
            """, nativeQuery = true)
    List<Request> fuzzySearchRequests(
            @Param("requestId") String requestId,
            @Param("customerName") String customerName,
            @Param("customerPhone") String customerPhone,
            @Param("customerEmail") String customerEmail,
            @Param("customerAddress") String customerAddress,
            @Param("brandModel") String brandModel,
            @Param("serialNumber") String serialNumber,
            @Param("description") String description,
            @Param("status") String status,
            @Param("appointmentDateFrom") String appointmentDateFrom,
            @Param("appointmentDateTo") String appointmentDateTo,
            @Param("completedDateFrom") String completedDateFrom,
            @Param("completedDateTo") String completedDateTo);
}
