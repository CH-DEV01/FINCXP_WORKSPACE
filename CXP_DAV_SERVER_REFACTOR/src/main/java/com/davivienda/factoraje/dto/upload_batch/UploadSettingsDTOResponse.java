package com.davivienda.factoraje.dto.upload_batch;

import java.util.List;

public record UploadSettingsDTOResponse(
        List<String> allowedExtensions,
        int maxFileSizeMb,
        int maxRows
) {
}
