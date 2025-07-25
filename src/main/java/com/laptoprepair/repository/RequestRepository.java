package com.laptoprepair.repository;

import com.laptoprepair.entity.Request;
import com.laptoprepair.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequestRepository extends JpaRepository<Request, UUID> {

       @Query("SELECT r FROM Request r " +
                     "LEFT JOIN FETCH r.items " +
                     "WHERE r.id = :id")
       Optional<Request> findByIdWithDetails(@Param("id") UUID id);

       @Query("SELECT r FROM Request r " +
                     "WHERE (:search IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%',:search,'%')) OR CAST(r.id AS string) LIKE %:search%) "
                     +
                     "AND (:status IS NULL OR r.status = :status) " +
                     "ORDER BY r.appointmentDate DESC")
       Page<Request> findWithFilters(@Param("search") String search,
                     @Param("status") RequestStatus status,
                     Pageable pageable);

       // Find requests by email for recovery
       @Query("SELECT r FROM Request r WHERE r.email = :email")
       List<Request> findByEmail(@Param("email") String email);
}