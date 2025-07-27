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

public interface RequestRepository extends JpaRepository<Request, UUID> {

       @Query("SELECT r FROM Request r WHERE r.id = :id")
       Optional<Request> findByIdWithDetails(@Param("id") UUID id);

       @Query(value = "SELECT * FROM request r " +
                     "WHERE (:search IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%',:search,'%'))) " +
                     "AND (:status IS NULL OR r.status = CAST(:status AS VARCHAR)) " +
                     "ORDER BY r.appointment_date DESC", countQuery = "SELECT COUNT(*) FROM request r " +
                                   "WHERE (:search IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%',:search,'%'))) " +
                                   "AND (:status IS NULL OR r.status = CAST(:status AS VARCHAR))", nativeQuery = true)
       Page<Request> findWithFilters(@Param("search") String search,
                     @Param("status") String status,
                     Pageable pageable);

       // Find requests by email for recovery
       @Query("SELECT r FROM Request r WHERE r.email = :email")
       List<Request> findByEmail(@Param("email") String email);
}