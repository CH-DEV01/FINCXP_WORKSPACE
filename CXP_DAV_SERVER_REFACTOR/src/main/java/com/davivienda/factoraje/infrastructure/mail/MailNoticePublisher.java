package com.davivienda.factoraje.infrastructure.mail;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.infrastructure.exception.UnauthorizedAccessException;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MailNoticePublisher {

    /** Valor anterior de un registro que se acaba de crear. */
    public static final String NO_RECORD = "Sin registro";

    private final ApplicationEventPublisher events;
    private final CurrentUserService currentUser;

    public void payerUploadSucceeded(UUID payerId, String payerName, UUID uploadBatchId, String fileName,
            int documentCount, String uploadedBy, List<MailNotice.SupplierDocuments> suppliers) {
        events.publishEvent(new MailNotice.PayerUploadSucceeded(payerId, payerName, uploadBatchId, fileName,
                documentCount, uploadedBy, List.copyOf(suppliers), Instant.now()));
    }

    public void uploadFailed(UUID payerId, String payerName, String fileName, String uploadedBy, String reason) {
        events.publishEvent(new MailNotice.UploadFailed(payerId, payerName, fileName, uploadedBy, reason));
    }

    public void operatorUploadSucceeded(UUID payerId, String payerName, UUID uploadBatchId, String fileName,
            int documentCount, String operatorName) {
        events.publishEvent(new MailNotice.OperatorUploadSucceeded(payerId, payerName, uploadBatchId, fileName,
                documentCount, operatorName, Instant.now()));
    }

    public void fundingRequested(String requestNumber, String supplierName, String payerName, String requestedBy,
            int documentCount, BigDecimal totalAmount, BigDecimal totalAmountToDisburse, LocalDate disbursementDate) {
        events.publishEvent(new MailNotice.FundingRequested(requestNumber, supplierName, payerName, requestedBy,
                documentCount, totalAmount, totalAmountToDisburse, disbursementDate, Instant.now()));
    }

    public void disbursementBatchCreated(String batchNumber, String payerName, LocalDate dueDate,
            LocalDate requestDate, LocalDate disbursementDate, int documentCount) {
        events.publishEvent(new MailNotice.DisbursementBatchCreated(
                batchNumber, payerName, dueDate, requestDate, disbursementDate, documentCount, Instant.now()));
    }

    public void dispersionBatchCreated(String batchNumber, String payerName, LocalDate dueDate, int documentCount) {
        events.publishEvent(new MailNotice.DispersionBatchCreated(
                batchNumber, payerName, dueDate, documentCount, Instant.now()));
    }

    public void operatorChanged(String menu, String previousValue, String newValue) {
        events.publishEvent(new MailNotice.OperatorChange(actorName(), menu, previousValue, newValue, Instant.now()));
    }

    private String actorName() {
        try {
            UserModel user = currentUser.get();
            String name = user.fullName();
            return name == null || name.isBlank() ? "Operador bancario" : name;
        } catch (UnauthorizedAccessException e) {
            return "Operador bancario";
        }
    }
}
