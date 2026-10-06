package com.davivienda.factoraje.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.davivienda.factoraje.dto.ApiResponse;
import com.davivienda.factoraje.dto.holiday.HolidayDTORequest;
import com.davivienda.factoraje.dto.holiday.HolidayDTOResponse;
import com.davivienda.factoraje.service.HolidayService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final HolidayService holidayService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HolidayDTOResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                holidayService.getHolidays(), "Días feriados obtenidos exitosamente."));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HolidayDTOResponse>> create(@Valid @RequestBody HolidayDTORequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                holidayService.createHoliday(request), "Día feriado registrado correctamente."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<HolidayDTOResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody HolidayDTORequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                holidayService.updateHoliday(id, request), "Día feriado actualizado correctamente."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        holidayService.deleteHoliday(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Día feriado eliminado correctamente."));
    }
}
