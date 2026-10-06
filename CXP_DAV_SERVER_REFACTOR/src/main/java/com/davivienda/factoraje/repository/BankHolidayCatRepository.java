package com.davivienda.factoraje.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.davivienda.factoraje.domain.catalogs.BankHolidayCat;

public interface BankHolidayCatRepository extends JpaRepository<BankHolidayCat, UUID> {
    
    @Query("SELECT b.holidayDate FROM BankHolidayCat b WHERE b.status = 'ACTIVE'")
    List<LocalDate> findActiveHolidayDates();

    boolean existsByHolidayDate(LocalDate holidayDate);

    boolean existsByHolidayDateAndIdNot(LocalDate holidayDate, UUID id);

}
