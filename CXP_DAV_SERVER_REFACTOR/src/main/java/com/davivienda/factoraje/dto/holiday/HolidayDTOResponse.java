package com.davivienda.factoraje.dto.holiday;

import java.time.LocalDate;
import java.util.UUID;

import com.davivienda.factoraje.domain.catalogs.BankHolidayCat;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

public record HolidayDTOResponse(
        UUID id,
        LocalDate holidayDate,
        String description,
        GeneralStatusEnum status
) {

    public static HolidayDTOResponse fromEntity(BankHolidayCat holiday) {
        return new HolidayDTOResponse(
                holiday.getId(),
                holiday.getHolidayDate(),
                holiday.getDescription(),
                holiday.getStatus());
    }
}
