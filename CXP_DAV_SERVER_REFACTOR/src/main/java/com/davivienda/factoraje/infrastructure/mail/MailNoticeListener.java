package com.davivienda.factoraje.infrastructure.mail;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.davivienda.factoraje.domain.constants.Roles;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.UploadBatchModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.infrastructure.exception.EmailDeliveryException;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.repository.UploadBatchRepository;
import com.davivienda.factoraje.repository.UserRepository;
import com.davivienda.factoraje.service.EmailService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MailNoticeListener {

    static final String PAYER_UPLOAD_SUBJECT =
            "Confirmación de carga exitosa: Archivo de Cuentas por Pagar procesado";
    static final String OPERATOR_UPLOAD_SUBJECT =
            "Confirmación de carga exitosa: Archivo de Cuentas por Pagar procesado contingentemente.";
    static final String UPLOAD_FAILED_SUBJECT =
            "Alerta de Sistema: Fallo en la carga del lote de Cuentas por Pagar de ";
    static final String SUPPLIER_SUBJECT = "¡Nuevas facturas disponibles para anticipo de pago con %s!";
    static final String FUNDING_REQUEST_SUBJECT = "Notificación Operativa: Nueva solicitud de anticipo de pago de %s";
    static final String DISBURSEMENT_SUBJECT =
            "Notificación Operativa: Lotes de desembolso listos para procesamiento";
    static final String DISPERSION_SUBJECT =
            "Notificación Operativa: Lotes de dispersión de pagos listos para descarga";
    static final String OPERATOR_CHANGE_SUBJECT =
            "Aviso de Auditoría: Cambios realizados en la Plataforma de Financiamiento";

    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy - HH:mm");

    private final EmailService emailService;
    private final MailTemplateRenderer templates;
    private final UserRepository userRepository;
    private final EntityRepository entityRepository;
    private final UploadBatchRepository uploadBatchRepository;
    private final SystemParameters systemParameters;
    private final ZoneId businessZone;

    public MailNoticeListener(EmailService emailService, MailTemplateRenderer templates,
            UserRepository userRepository, EntityRepository entityRepository,
            UploadBatchRepository uploadBatchRepository, SystemParameters systemParameters,
            @Value("${app.business-zone:America/El_Salvador}") ZoneId businessZone) {
        this.emailService = emailService;
        this.templates = templates;
        this.userRepository = userRepository;
        this.entityRepository = entityRepository;
        this.uploadBatchRepository = uploadBatchRepository;
        this.systemParameters = systemParameters;
        this.businessZone = businessZone;
    }

    @Async("mailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotice(MailNotice notice) {
        if (!emailService.isConfigured()) {
            log.warn("Correo no enviado ({}): Mailjet no está configurado.", notice.getClass().getSimpleName());
            return;
        }
        try {
            switch (notice) {
                case MailNotice.PayerUploadSucceeded event -> payerUploadSucceeded(event);
                case MailNotice.UploadFailed event -> uploadFailed(event);
                case MailNotice.OperatorUploadSucceeded event -> operatorUploadSucceeded(event);
                case MailNotice.FundingRequested event -> fundingRequested(event);
                case MailNotice.DisbursementBatchCreated event -> disbursementBatchCreated(event);
                case MailNotice.DispersionBatchCreated event -> dispersionBatchCreated(event);
                case MailNotice.OperatorChange event -> operatorChange(event);
            }
        } catch (RuntimeException e) {
            log.error("No se pudo preparar el correo {}: {}", notice.getClass().getSimpleName(), e.getMessage());
        }
    }

    private void payerUploadSucceeded(MailNotice.PayerUploadSucceeded event) {
        String payerName = text(event.payerName());
        String batchNumber = batchNumber(event.uploadBatchId());
        String uploadDate = shortDate(event.occurredAt());
        Map<String, String> values = Map.of(
                "payerName", payerName,
                "batchNumber", batchNumber,
                "uploadDate", uploadDate);
        String body = """
                Le informamos que la carga de su archivo de cuentas por pagar de %s se ha procesado de manera exitosa \
                en nuestra Plataforma de Financiamiento de Cuentas por Pagar.
                LOTE_ID: %s
                Estado: Procesado Exitosamente
                Fecha de carga: %s
                """.formatted(payerName, batchNumber, uploadDate);
        sendEach(union(entityRecipients(event.payerId()), adminRecipients()),
                PAYER_UPLOAD_SUBJECT, "carga-exitosa.html", values, body);

        String loginUrl = loginUrl();
        if (loginUrl == null) {
            log.warn("APP_LOGIN_URL no está configurada; no se envió el correo a los proveedores de {}.", payerName);
            return;
        }
        String subject = SUPPLIER_SUBJECT.formatted(payerName);
        for (MailNotice.SupplierDocuments supplier : event.suppliers()) {
            List<MailRecipient> recipients = supplierRecipients(supplier);
            if (recipients.isEmpty()) {
                log.info("El proveedor {} no tiene usuarios activos; no se envió el correo de la carga.", supplier.nit());
                continue;
            }
            Map<String, String> supplierValues = Map.of(
                    "payerName", payerName,
                    "supplierName", supplierName(supplier),
                    "availableDate", uploadDate,
                    "loginUrl", loginUrl);
            String supplierText = """
                    Le informamos que %s ha cargado un nuevo lote de facturas aprobadas en nuestra Plataforma \
                    de Financiamiento de Cuentas por Pagar.
                    Fecha de disponibilidad: %s
                    Para gestionar sus facturas y solicitar su anticipo, ingrese a la plataforma: %s
                    """.formatted(payerName, uploadDate, loginUrl);
            sendEach(recipients, subject, "carga-exitosa-proveedor.html", supplierValues, supplierText);
        }
    }

    private void uploadFailed(MailNotice.UploadFailed event) {
        String payerName = payerName(event.payerId(), event.payerName());
        Map<String, String> values = Map.of(
                "payerName", payerName,
                "fileName", text(event.fileName()),
                "reason", text(event.reason()));
        String body = """
                Le notificamos que el sistema ha registrado un error durante el intento de carga del último lote \
                de cuentas por pagar de %s en la Plataforma de Financiamiento.
                Archivo: %s
                Estado: Carga Fallida
                Acción requerida: Verificar formato y reporte de errores
                Detalle: %s
                Para asistencia inmediata, por favor contactarse al servicio de atención al cliente al 2556-2115.
                """.formatted(payerName, text(event.fileName()), text(event.reason()));
        sendEach(union(entityRecipients(event.payerId()), adminRecipients()),
                UPLOAD_FAILED_SUBJECT + payerName, "carga-fallida.html", values, body);
    }

    private void operatorUploadSucceeded(MailNotice.OperatorUploadSucceeded event) {
        String payerName = text(event.payerName());
        String operatorName = text(event.operatorName());
        String batchNumber = batchNumber(event.uploadBatchId());
        Map<String, String> values = Map.of(
                "payerName", payerName,
                "operatorName", operatorName,
                "batchNumber", batchNumber);
        String body = """
                Le informamos que su solicitud contingencial de carga de archivo de cuentas por pagar de %s se ha \
                procesado de manera exitosa en nuestra Plataforma de Financiamiento de Cuentas por Pagar por %s.
                LOTE_ID: %s
                Estado: Procesado Exitosamente
                """.formatted(payerName, operatorName, batchNumber);
        List<MailRecipient> payerUsers = entityRecipients(event.payerId());
        List<MailRecipient> admins = excluding(adminRecipients(), payerUsers);
        if (payerUsers.isEmpty()) {
            log.info("El pagador {} no tiene usuarios activos; la copia de la carga queda para los operadores.", payerName);
            sendEach(admins, OPERATOR_UPLOAD_SUBJECT, "carga-operador.html", values, body);
            return;
        }
        String greeting = payerUsers.stream().map(MailRecipient::name).collect(Collectors.joining(", "));
        String html = templates.render("carga-operador.html", withRecipient(values, greeting));
        try {
            emailService.send(new OutboundMail(payerUsers, admins, OPERATOR_UPLOAD_SUBJECT, html, body));
        } catch (EmailDeliveryException e) {
            log.error("No se envió {}: {}", OPERATOR_UPLOAD_SUBJECT, e.getMessage());
        }
    }

    private void fundingRequested(MailNotice.FundingRequested event) {
        String supplierName = text(event.supplierName());
        String requestedAt = dateTime(event.occurredAt());
        Map<String, String> values = new HashMap<>();
        values.put("requestNumber", text(event.requestNumber()));
        values.put("supplierName", supplierName);
        values.put("payerName", text(event.payerName()));
        values.put("requestedBy", text(event.requestedBy()));
        values.put("documentCount", String.valueOf(event.documentCount()));
        values.put("totalAmount", AuditText.amount(event.totalAmount()));
        values.put("totalAmountToDisburse", AuditText.amount(event.totalAmountToDisburse()));
        values.put("disbursementDate", AuditText.date(event.disbursementDate()));
        values.put("requestedAt", requestedAt);
        putLoginUrl(values);
        String body = """
                El sistema le notifica que %s ha enviado una nueva solicitud de anticipo de pago en la Plataforma \
                de Financiamiento de Cuentas por Pagar.
                Solicitud: %s
                Pagador: %s
                Solicitado por: %s
                Documentos: %s
                Monto de los documentos: %s
                Monto a desembolsar: %s
                Fecha de desembolso programada: %s
                Fecha y hora de la solicitud: %s
                """.formatted(supplierName, values.get("requestNumber"), values.get("payerName"),
                values.get("requestedBy"), values.get("documentCount"), values.get("totalAmount"),
                values.get("totalAmountToDisburse"), values.get("disbursementDate"), requestedAt)
                + loginLine(values);
        sendEach(adminRecipients(), FUNDING_REQUEST_SUBJECT.formatted(supplierName),
                "solicitud-anticipo.html", values, body);
    }

    private void disbursementBatchCreated(MailNotice.DisbursementBatchCreated event) {
        String generatedDate = shortDate(event.occurredAt());
        Map<String, String> values = new HashMap<>();
        values.put("batchNumber", text(event.batchNumber()));
        values.put("payerName", text(event.payerName()));
        values.put("generatedDate", generatedDate);
        putLoginUrl(values);
        String body = """
                El sistema le notifica que se han generado nuevos lotes de desembolso (solicitudes de anticipo de pago) \
                en la Plataforma de Financiamiento de Cuentas por Pagar.
                Lote: %s
                Pagador: %s
                Tipo de archivo: Solicitudes de anticipo
                Fecha de generación: %s
                """.formatted(text(event.batchNumber()), text(event.payerName()), generatedDate) + loginLine(values);
        sendEach(adminRecipients(), DISBURSEMENT_SUBJECT, "lote-desembolsos.html", values, body);
    }

    private void dispersionBatchCreated(MailNotice.DispersionBatchCreated event) {
        Map<String, String> values = new HashMap<>();
        values.put("batchNumber", text(event.batchNumber()));
        values.put("payerName", text(event.payerName()));
        putLoginUrl(values);
        String body = """
                Le informamos que los lotes de dispersión de pagos ya están disponibles para su descarga en la plataforma.
                Lote: %s
                Pagador: %s
                Tipo de archivo: Abono directo a proveedores
                """.formatted(text(event.batchNumber()), text(event.payerName())) + loginLine(values);
        sendEach(adminRecipients(), DISPERSION_SUBJECT, "lote-dispersiones.html", values, body);
    }

    private void operatorChange(MailNotice.OperatorChange event) {
        String occurredAt = dateTime(event.occurredAt());
        Map<String, String> values = Map.of(
                "actorName", text(event.actorName()),
                "menu", text(event.menu()),
                "occurredAt", occurredAt,
                "previousValue", text(event.previousValue()),
                "newValue", text(event.newValue()));
        String body = """
                El sistema de la Plataforma de Financiamiento de Cuentas por Pagar ha registrado una modificación.
                Módulo: %s
                Usuario que realizó el cambio: %s
                Fecha y Hora: %s
                Valor Anterior: %s
                Nuevo Valor: %s
                """.formatted(text(event.menu()), text(event.actorName()), occurredAt,
                text(event.previousValue()), text(event.newValue()));
        sendEach(adminRecipients(), OPERATOR_CHANGE_SUBJECT, "alerta-cambio.html", values, body);
    }

    private String batchNumber(UUID uploadBatchId) {
        if (uploadBatchId == null) {
            return "—";
        }
        return uploadBatchRepository.findById(uploadBatchId)
                .map(UploadBatchModel::getBatchNumber)
                .map(MailNoticeListener::text)
                .orElse("—");
    }

    private String loginUrl() {
        String url = systemParameters.get(SystemParameterKey.APP_LOGIN_URL);
        if (url == null || url.isBlank() || SystemParameterKey.UNCONFIGURED.equals(url)) {
            return null;
        }
        return url;
    }

    private void putLoginUrl(Map<String, String> values) {
        String url = loginUrl();
        if (url != null) {
            values.put("loginUrl", url);
        }
    }

    private static String loginLine(Map<String, String> values) {
        String url = values.get("loginUrl");
        return url == null ? "" : "Ingrese a la plataforma: " + url + "\n";
    }

    private List<MailRecipient> supplierRecipients(MailNotice.SupplierDocuments supplier) {
        return entityRepository.findByNit(supplier.nit())
                .map(entity -> entityRecipients(entity.getId()))
                .orElseGet(() -> {
                    log.info("No hay un proveedor registrado con NIT {}; no se envió su correo de carga.", supplier.nit());
                    return List.of();
                });
    }

    private String supplierName(MailNotice.SupplierDocuments supplier) {
        return entityRepository.findByNit(supplier.nit())
                .map(EntityModel::getName)
                .filter(name -> name != null && !name.isBlank())
                .orElse(text(supplier.supplierName()));
    }

    private String payerName(UUID payerId, String provided) {
        if (provided != null && !provided.isBlank()) {
            return provided;
        }
        if (payerId == null) {
            return "el pagador";
        }
        return entityRepository.findById(payerId).map(EntityModel::getName).orElse("el pagador");
    }

    private List<MailRecipient> entityRecipients(UUID entityId) {
        if (entityId == null) {
            return List.of();
        }
        return recipientsOf(userRepository.findActiveByEntityId(entityId));
    }

    private List<MailRecipient> adminRecipients() {
        return recipientsOf(userRepository.findActiveByRoleName(Roles.ADMIN));
    }

    private List<MailRecipient> recipientsOf(List<UserModel> users) {
        Map<String, MailRecipient> unique = new LinkedHashMap<>();
        for (UserModel user : users) {
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                continue;
            }
            String email = user.getEmail().toLowerCase(Locale.ROOT);
            String name = user.fullName();
            unique.putIfAbsent(email, new MailRecipient(email, name == null || name.isBlank() ? email : name));
        }
        return List.copyOf(unique.values());
    }

    private static List<MailRecipient> union(List<MailRecipient> first, List<MailRecipient> second) {
        Map<String, MailRecipient> unique = new LinkedHashMap<>();
        for (MailRecipient recipient : first) {
            unique.put(recipient.email(), recipient);
        }
        for (MailRecipient recipient : second) {
            unique.putIfAbsent(recipient.email(), recipient);
        }
        return List.copyOf(unique.values());
    }

    private static List<MailRecipient> excluding(List<MailRecipient> recipients, List<MailRecipient> alreadyIncluded) {
        List<String> taken = alreadyIncluded.stream().map(MailRecipient::email).toList();
        List<MailRecipient> remaining = new ArrayList<>();
        for (MailRecipient recipient : recipients) {
            if (!taken.contains(recipient.email())) {
                remaining.add(recipient);
            }
        }
        return List.copyOf(remaining);
    }

    /** Cada destinatario recibe su propio correo con el saludo a su nombre. */
    private void sendEach(List<MailRecipient> recipients, String subject, String template,
            Map<String, String> values, String text) {
        if (recipients.isEmpty()) {
            log.info("Correo no enviado ({}): no hay destinatarios.", subject);
            return;
        }
        for (MailRecipient recipient : recipients) {
            String html = templates.render(template, withRecipient(values, recipient.name()));
            try {
                emailService.send(new OutboundMail(List.of(recipient), List.of(), subject, html, text));
            } catch (EmailDeliveryException e) {
                log.error("No se envió {} a {}: {}", subject, recipient.email(), e.getMessage());
            }
        }
    }

    private static Map<String, String> withRecipient(Map<String, String> values, String recipientName) {
        Map<String, String> personalized = new HashMap<>(values);
        personalized.put("recipientName", text(recipientName));
        return personalized;
    }

    private String shortDate(Instant instant) {
        return instant == null ? "—" : SHORT_DATE.format(instant.atZone(businessZone));
    }

    private String dateTime(Instant instant) {
        return instant == null ? "—" : DATE_TIME.format(instant.atZone(businessZone));
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
