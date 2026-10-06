package com.davivienda.factoraje.infrastructure.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.davivienda.factoraje.domain.constants.Roles;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.UploadBatchModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;
import com.davivienda.factoraje.domain.enums.SystemParameterKey;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.repository.UploadBatchRepository;
import com.davivienda.factoraje.repository.UserRepository;
import com.davivienda.factoraje.service.EmailService;

class MailNoticeListenerTest {

    private static final String LOGIN = "https://devpay.davivienda.com.sv";
    private static final LocalDate DUE = LocalDate.of(2026, 10, 8);
    /** 12:30 del 4 de octubre de 2026 en El Salvador. */
    private static final Instant OCCURRED_AT = Instant.parse("2026-10-04T18:30:00Z");

    private final EmailService emailService = org.mockito.Mockito.mock(EmailService.class);
    private final UserRepository userRepository = org.mockito.Mockito.mock(UserRepository.class);
    private final EntityRepository entityRepository = org.mockito.Mockito.mock(EntityRepository.class);
    private final UploadBatchRepository uploadBatchRepository = org.mockito.Mockito.mock(UploadBatchRepository.class);
    private final SystemParameters systemParameters = org.mockito.Mockito.mock(SystemParameters.class);

    private final UUID payerId = UUID.randomUUID();
    private final UUID supplierId = UUID.randomUUID();
    private final UUID uploadBatchId = UUID.randomUUID();
    private final UserModel payerUser = user("pagador@empresa.com", "Ana", "Pagadora");
    private final UserModel admin = user("operador@banco.com", "Luis", "Banco");
    private final UserModel supplierUser = user("proveedor@empresa.com", "Marta", "Provee");

    private MailNoticeListener listener;

    @BeforeEach
    void setUp() {
        listener = new MailNoticeListener(emailService, new MailTemplateRenderer(), userRepository, entityRepository,
                uploadBatchRepository, systemParameters, ZoneId.of("America/El_Salvador"));
        when(emailService.isConfigured()).thenReturn(true);
        when(systemParameters.get(SystemParameterKey.APP_LOGIN_URL)).thenReturn(LOGIN);
        when(userRepository.findActiveByEntityId(payerId)).thenReturn(List.of(payerUser));
        when(userRepository.findActiveByRoleName(Roles.ADMIN)).thenReturn(List.of(admin));
        when(userRepository.findActiveByEntityId(supplierId)).thenReturn(List.of(supplierUser));
        when(entityRepository.findByNit("06140101901011")).thenReturn(Optional.of(
                EntityModel.builder().id(supplierId).name("Proveedor Uno").nit("06140101901011").build()));
        when(uploadBatchRepository.findById(uploadBatchId)).thenReturn(Optional.of(
                UploadBatchModel.builder().id(uploadBatchId).batchNumber("CARGA-20261004-7").build()));
    }

    @Test
    void successfulPayerUploadGreetsEachRecipientAndInvitesSuppliersWithTheAccessButton() {
        listener.onNotice(new MailNotice.PayerUploadSucceeded(
                payerId, "Pagador Demo", uploadBatchId, "facturas.xlsx", 3, "Ana Pagadora",
                List.of(new MailNotice.SupplierDocuments("06140101901011", "Proveedor del archivo", 2)), OCCURRED_AT));

        List<OutboundMail> mails = sent();
        assertThat(mails).hasSize(3);
        List<OutboundMail> payerMails = mails.stream()
                .filter(mail -> mail.subject().equals(MailNoticeListener.PAYER_UPLOAD_SUBJECT))
                .toList();
        assertThat(payerMails).extracting(mail -> mail.to().getFirst().email())
                .containsExactlyInAnyOrder("pagador@empresa.com", "operador@banco.com");
        assertThat(payerMails).allSatisfy(mail -> assertThat(mail.html())
                .contains("LOTE_ID: CARGA-20261004-7")
                .contains("Procesado Exitosamente")
                .contains("04/10/26")
                .doesNotContain("Ingresar a la plataforma")
                .doesNotContain("{{"));
        assertThat(htmlFor(payerMails, "pagador@empresa.com")).contains("Estimado/a Ana Pagadora");
        assertThat(htmlFor(payerMails, "operador@banco.com")).contains("Estimado/a Luis Banco");

        OutboundMail supplierMail = mails.stream()
                .filter(mail -> mail.to().getFirst().email().equals("proveedor@empresa.com"))
                .findFirst()
                .orElseThrow();
        assertThat(supplierMail.subject()).isEqualTo("¡Nuevas facturas disponibles para anticipo de pago con Pagador Demo!");
        assertThat(supplierMail.html())
                .contains("Estimado/a Marta Provee")
                .contains("Proveedor Uno")
                .contains("Pagador: Pagador Demo")
                .contains("Ingresar a la plataforma")
                .contains("href=\"" + LOGIN + "\"")
                .doesNotContain("<!--if:")
                .doesNotContain("{{");
    }

    @Test
    void rejectedUploadAlertsPayerAndBankUsersWithoutNotifyingSuppliers() {
        listener.onNotice(new MailNotice.UploadFailed(
                payerId, "Pagador Demo", "facturas.xlsx", "Ana Pagadora", "El archivo tiene 2 inconsistencias."));

        List<OutboundMail> mails = sent();
        assertThat(mails).extracting(mail -> mail.to().getFirst().email())
                .containsExactlyInAnyOrder("pagador@empresa.com", "operador@banco.com");
        assertThat(mails).allSatisfy(mail -> {
            assertThat(mail.subject()).isEqualTo(
                    "Alerta de Sistema: Fallo en la carga del lote de Cuentas por Pagar de Pagador Demo");
            assertThat(mail.html())
                    .contains("Carga Fallida")
                    .contains("Archivo: facturas.xlsx")
                    .contains("El archivo tiene 2 inconsistencias.")
                    .contains("2556-2115");
        });
        verify(userRepository, never()).findActiveByEntityId(supplierId);
    }

    @Test
    void operatorUploadGoesToPayerUsersWithBankUsersInCopy() {
        listener.onNotice(new MailNotice.OperatorUploadSucceeded(
                payerId, "Pagador Demo", uploadBatchId, "facturas.xlsx", 4, "Luis Banco", OCCURRED_AT));

        OutboundMail mail = sent().getFirst();
        assertThat(mail.subject()).isEqualTo(MailNoticeListener.OPERATOR_UPLOAD_SUBJECT);
        assertThat(mail.to()).extracting(MailRecipient::email).containsExactly("pagador@empresa.com");
        assertThat(mail.cc()).extracting(MailRecipient::email).containsExactly("operador@banco.com");
        assertThat(mail.html())
                .contains("Estimado/a Ana Pagadora")
                .contains("Carga Contingencial de Archivo")
                .contains("LOTE_ID: CARGA-20261004-7")
                .contains("Luis Banco");
    }

    @Test
    void fundingRequestNotifiesOnlyBankUsers() {
        listener.onNotice(new MailNotice.FundingRequested(
                "REQ-1A2B3C4D", "Proveedor Uno", "Pagador Demo", "Marta Provee", 3,
                new java.math.BigDecimal("1500.00"), new java.math.BigDecimal("1462.35"), DUE, OCCURRED_AT));

        List<OutboundMail> mails = sent();
        assertThat(mails).hasSize(1);
        OutboundMail mail = mails.getFirst();
        assertThat(mail.subject())
                .isEqualTo("Notificación Operativa: Nueva solicitud de anticipo de pago de Proveedor Uno");
        assertThat(mail.to()).extracting(MailRecipient::email).containsExactly("operador@banco.com");
        assertThat(mail.html())
                .contains("Estimado Luis Banco")
                .contains("Solicitud: REQ-1A2B3C4D")
                .contains("Pagador Demo")
                .contains("Marta Provee")
                .contains("$1,500.00")
                .contains("$1,462.35")
                .contains("08/10/2026")
                .contains("04/10/2026 - 12:30")
                .contains("href=\"" + LOGIN + "\"")
                .doesNotContain("{{");
        verify(userRepository, never()).findActiveByEntityId(payerId);
    }

    @Test
    void disbursementBatchNotifiesBankUsersWithTheAccessButton() {
        listener.onNotice(new MailNotice.DisbursementBatchCreated(
                "DES-20261004-1", "Pagador Demo", DUE, DUE.minusDays(2), DUE.minusDays(1), 5, OCCURRED_AT));

        OutboundMail mail = sent().getFirst();
        assertThat(mail.subject()).isEqualTo(MailNoticeListener.DISBURSEMENT_SUBJECT);
        assertThat(mail.to()).extracting(MailRecipient::email).containsExactly("operador@banco.com");
        assertThat(mail.html())
                .contains("Estimado Luis Banco")
                .contains("DES-20261004-1")
                .contains("Solicitudes de anticipo")
                .contains("04/10/26")
                .contains("href=\"" + LOGIN + "\"");
    }

    @Test
    void dispersionBatchOmitsTheButtonWhenTheLoginUrlIsNotConfigured() {
        when(systemParameters.get(SystemParameterKey.APP_LOGIN_URL)).thenReturn(SystemParameterKey.UNCONFIGURED);

        listener.onNotice(new MailNotice.DispersionBatchCreated("DSP-20261004-1", "Pagador Demo", DUE, 6, OCCURRED_AT));

        OutboundMail mail = sent().getFirst();
        assertThat(mail.subject()).isEqualTo(MailNoticeListener.DISPERSION_SUBJECT);
        assertThat(mail.html())
                .contains("DSP-20261004-1")
                .contains("Abono directo a proveedores")
                .doesNotContain("Ingresar a la plataforma")
                .doesNotContain("{{");
    }

    @Test
    void operatorChangeShowsPreviousAndNewValues() {
        listener.onNotice(new MailNotice.OperatorChange("Luis Banco", "Feriados",
                "Feriado del 25/12/2026: Navidad", "Feriado eliminado", OCCURRED_AT));

        OutboundMail mail = sent().getFirst();
        assertThat(mail.subject()).isEqualTo(MailNoticeListener.OPERATOR_CHANGE_SUBJECT);
        assertThat(mail.html())
                .contains("Estimado Luis Banco,")
                .contains("Módulo: Feriados")
                .contains("04/10/2026 - 12:30")
                .contains("Feriado del 25/12/2026: Navidad")
                .contains("Feriado eliminado");
    }

    @Test
    void skipsSupplierMailWhenTheLoginUrlIsNotConfigured() {
        when(systemParameters.get(SystemParameterKey.APP_LOGIN_URL)).thenReturn(SystemParameterKey.UNCONFIGURED);

        listener.onNotice(new MailNotice.PayerUploadSucceeded(
                payerId, "Pagador Demo", uploadBatchId, "facturas.xlsx", 1, "Ana",
                List.of(new MailNotice.SupplierDocuments("06140101901011", "Proveedor", 1)), OCCURRED_AT));

        assertThat(sent()).extracting(mail -> mail.to().getFirst().email())
                .containsExactlyInAnyOrder("pagador@empresa.com", "operador@banco.com");
    }

    @Test
    void doesNotCallMailjetWhenCredentialsAreMissing() {
        when(emailService.isConfigured()).thenReturn(false);

        listener.onNotice(new MailNotice.OperatorChange("Luis Banco", "Usuarios",
                MailNoticePublisher.NO_RECORD, "Usuario nuevo", OCCURRED_AT));

        verify(emailService, never()).send(any());
    }

    private static String htmlFor(List<OutboundMail> mails, String email) {
        return mails.stream()
                .filter(mail -> mail.to().getFirst().email().equals(email))
                .findFirst()
                .orElseThrow()
                .html();
    }

    private List<OutboundMail> sent() {
        ArgumentCaptor<OutboundMail> captor = ArgumentCaptor.forClass(OutboundMail.class);
        verify(emailService, org.mockito.Mockito.atLeastOnce()).send(captor.capture());
        return captor.getAllValues();
    }

    private static UserModel user(String email, String firstName, String lastName) {
        return UserModel.builder()
                .id(UUID.randomUUID())
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .status(GeneralStatusEnum.ACTIVE)
                .build();
    }
}
