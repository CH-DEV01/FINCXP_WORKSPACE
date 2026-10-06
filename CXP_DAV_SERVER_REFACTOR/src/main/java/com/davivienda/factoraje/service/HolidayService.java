package com.davivienda.factoraje.service;

import java.util.List;
import java.util.UUID;

import com.davivienda.factoraje.dto.holiday.HolidayDTORequest;
import com.davivienda.factoraje.dto.holiday.HolidayDTOResponse;

public interface HolidayService {

    List<HolidayDTOResponse> getHolidays();

    HolidayDTOResponse createHoliday(HolidayDTORequest request);

    HolidayDTOResponse updateHoliday(UUID id, HolidayDTORequest request);

    void deleteHoliday(UUID id);
}
