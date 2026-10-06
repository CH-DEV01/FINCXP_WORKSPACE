package com.davivienda.factoraje.service.impl;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.davivienda.factoraje.domain.entities.DispersionBatchModel;
import com.davivienda.factoraje.domain.entities.DocumentModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.UploadBatchModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.dto.dispersion.DispersionBatchSummaryDTOResponse;
import com.davivienda.factoraje.dto.report.DispersionLetterData;
import com.davivienda.factoraje.infrastructure.util.Money;

import lombok.RequiredArgsConstructor;

/** Arma los datos de la carta de solicitud de dispersión de un lote. */
@Component
@RequiredArgsConstructor
public class DispersionLetterAssembler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter LONG_DATE_FMT =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-SV"));

    private final DispersionDocuments dispersionDocuments;

    public String fileName(DispersionBatchModel batch) {
        return "Solicitud_Dispersion_" + batch.getBatchNumber() + ".pdf";
    }

    public DispersionLetterData assemble(DispersionBatchModel batch, List<DocumentModel> documents) {
        String dispersionDate = DATE_FMT.format(batch.getDispersionDate());
        Map<UUID, String> accounts = dispersionDocuments.mainAccounts(documents.stream()
                .map(doc -> doc.getMasterAgreement().getSupplier().getId())
                .distinct()
                .toList());

        Map<List<Object>, List<DocumentModel>> bySupplierAndDueDate = documents.stream()
                .sorted(DispersionDocuments.LETTER_ORDER)
                .collect(Collectors.groupingBy(
                        doc -> Arrays.asList(doc.getMasterAgreement().getSupplier().getId(), doc.getDueDate()),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<DispersionLetterData.Row> rows = bySupplierAndDueDate.values().stream()
                .map(group -> {
                    DocumentModel first = group.getFirst();
                    EntityModel supplier = first.getMasterAgreement().getSupplier();
                    String account = accounts.get(supplier.getId());
                    BigDecimal total = group.stream()
                            .map(DocumentModel::getNominalAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new DispersionLetterData.Row(
                            dispersionDate,
                            supplier.getName(),
                            account != null ? account : "Sin cuenta principal",
                            String.valueOf(group.size()),
                            first.getDueDate() != null ? DATE_FMT.format(first.getDueDate()) : "-",
                            Money.format(total));
                })
                .toList();

        UserModel signer = batch.getSigner();

        return new DispersionLetterData(
                batch.getBatchNumber(),
                LONG_DATE_FMT.format(batch.getDispersionDate()),
                DispersionBatchSummaryDTOResponse.fullName(signer),
                signer != null ? signer.getDui() : null,
                batch.getPayer().getName(),
                batch.getPayerAccountNumber(),
                rows);
    }

    /**
     * Usuario del pagador que cargó y aprobó los documentos del lote. Si vienen de
     * varias cargas, se toma la más reciente.
     */
    static UserModel signerOf(List<DocumentModel> documents) {
        return documents.stream()
                .map(DocumentModel::getUploadBatch)
                .filter(Objects::nonNull)
                .max(Comparator.comparing(UploadBatchModel::getCreatedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(UploadBatchModel::getUploadedAndApprovedBy)
                .orElse(null);
    }
}
