package com.davivienda.factoraje.repository;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.davivienda.factoraje.domain.entities.PendingSessionModel;

@Repository
public interface PendingSessionRepository extends JpaRepository<PendingSessionModel, String> {

    @Modifying
    @Query("DELETE FROM PendingSessionModel p WHERE p.expiresAt < :now")
    void deleteExpiredSessions(@Param("now") LocalDateTime now);

}
