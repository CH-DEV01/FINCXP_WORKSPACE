package com.davivienda.factoraje.service.impl;

import static com.davivienda.factoraje.service.impl.DisbursementSummaries.groupBySupplier;
import static com.davivienda.factoraje.service.impl.DisbursementSummaries.sumCents;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.davivienda.factoraje.domain.entities.CreditFacilityModel;
import com.davivienda.factoraje.domain.entities.DisbursementBatchModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.FinancingTransactionModel;
import com.davivienda.factoraje.domain.entities.UploadBatchModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.dto.disbursement_batch.DisbursementRequestSupplierDTOResponse;
import com.davivienda.factoraje.dto.report.DisbursementLetterData;
import com.davivienda.factoraje.infrastructure.exception.ResourceNotFoundException;
import com.davivienda.factoraje.infrastructure.util.Money;
import com.davivienda.factoraje.repository.CreditFacilityRepository;

import lombok.RequiredArgsConstructor;

/** Arma los datos de la carta de solicitud de desembolso de un lote. */
@Component
@RequiredArgsConstructor
public class DisbursementLetterAssembler {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter LONG_DATE_FMT =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-SV"));

    private final CreditFacilityRepository creditFacilityRepository;
    private final DisbursementSummaries summaries;

    public record ReportContext(String payerName, String creditFacilityNumber) {}

    public ReportContext loadReportContext(EntityModel payer) {

        CreditFacilityModel creditFacility = creditFacilityRepository.findByPayerId(payer.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No se encontró la línea de crédito del pagador con ID: " + payer.getId()));

        return new ReportContext(payer.getName(), creditFacility.getCreditFacilityNumber());
    }

    public String reportFileName(DisbursementBatchModel batch) {
        return "Reporte_Desembolso_" + batch.getBatchNumber() + ".pdf";
    }

    /**
     * Carta de solicitud de desembolso con una fila por proveedor. El monto de la
     * prosa es la suma de los montos ya redondeados de la tabla, para que cuadren.
     */
    public DisbursementLetterData assemble(DisbursementBatchModel batch, List<FinancingTransactionModel> transactions,
            ReportContext context) {

        Map<UUID, List<FinancingTransactionModel>> bySupplier = groupBySupplier(transactions);
        LocalDate disbursementDate = summaries.disbursementDate(batch, transactions);
        String letterDate = DATE_FMT.format(disbursementDate);

        BigDecimal totalToDisburse = BigDecimal.ZERO;
        List<DisbursementLetterData.Row> rows = new ArrayList<>();

        for (DisbursementRequestSupplierDTOResponse supplier : summaries.summarizeBySupplier(transactions)) {
            List<FinancingTransactionModel> txs = bySupplier.get(supplier.supplierId());
            BigDecimal amountToDisburse = sumCents(txs, FinancingTransactionModel::getAmountToFinance);
            totalToDisburse = totalToDisburse.add(amountToDisburse);

            rows.add(new DisbursementLetterData.Row(
                    letterDate,
                    financingDays(txs.getFirst()),
                    supplier.dueDate() != null ? DATE_FMT.format(supplier.dueDate()) : "-",
                    Money.format(sumCents(txs, FinancingTransactionModel::getFlatAmount)),
                    Money.format(sumCents(txs, FinancingTransactionModel::getInterestAmount)),
                    Money.format(amountToDisburse),
                    Money.format(sumCents(txs, tx -> tx.getCommissionAmount().add(tx.getIvaAmount()))),
                    Money.format(supplier.amountToCredit()),
                    supplier.accountNumber() != null ? supplier.accountNumber() : "Sin cuenta principal",
                    supplier.supplierName()));
        }

        UserModel signer = letterSigner(transactions);

        return new DisbursementLetterData(
                batch.getBatchNumber(),
                LONG_DATE_FMT.format(disbursementDate),
                signer != null ? fullName(signer) : null,
                signer != null ? signer.getDui() : null,
                context.payerName(),
                context.creditFacilityNumber(),
                Money.format(totalToDisburse),
                rows);
    }

    /** Días financiados con el mismo conteo inclusivo usado para calcular el interés. */
    private String financingDays(FinancingTransactionModel tx) {
        LocalDate disbursementDate = tx.getScheduledDisbursementDate();
        LocalDate dueDate = tx.getDocument().getDueDate();
        if (disbursementDate == null || dueDate == null) {
            return "-";
        }
        return String.valueOf(ChronoUnit.DAYS.between(disbursementDate, dueDate) + 1);
    }

    /**
     * Usuario del pagador que cargó y aprobó el archivo de cuentas por pagar. Si el
     * lote mezcla varias cargas, se toma la más reciente.
     */
    private UserModel letterSigner(List<FinancingTransactionModel> transactions) {
        return transactions.stream()
                .map(tx -> tx.getDocument().getUploadBatch())
                .filter(Objects::nonNull)
                .max(Comparator.comparing(UploadBatchModel::getCreatedAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(UploadBatchModel::getUploadedAndApprovedBy)
                .orElse(null);
    }

    private String fullName(UserModel user) {
        return Stream.of(user.getFirstName(), user.getLastName())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
    }
}
