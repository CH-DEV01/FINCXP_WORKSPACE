package com.davivienda.factoraje.dto.upload_resource;

import java.time.Instant;

/** {@code template} o {@code manual} son null mientras el ADMIN no los publique. */
public record UploadResourcesDTOResponse(ResourceInfo template, ResourceInfo manual) {

    public record ResourceInfo(String fileName, long fileSize, Instant updatedAt, String updatedBy) {
    }
}
