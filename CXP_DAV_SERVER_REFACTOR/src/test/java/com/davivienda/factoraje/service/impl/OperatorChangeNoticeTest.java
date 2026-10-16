package com.davivienda.factoraje.service.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.davivienda.factoraje.domain.catalogs.DisbursementPolicyCat;
import com.davivienda.factoraje.domain.catalogs.EntityTypeCat;
import com.davivienda.factoraje.domain.catalogs.PaymentPolicyCat;
import com.davivienda.factoraje.domain.catalogs.RoleCat;
import com.davivienda.factoraje.domain.constants.EntityTypeCode;
import com.davivienda.factoraje.domain.constants.Roles;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.MasterAgreementModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.dto.holiday.HolidayDTORequest;
import com.davivienda.factoraje.dto.master_agreement.MasterAgreementDTORequest;
import com.davivienda.factoraje.dto.user.UserDTORequest;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.repository.BankAccountRepository;
import com.davivienda.factoraje.repository.BankHolidayCatRepository;
import com.davivienda.factoraje.repository.DisbursementPolicyCatRepository;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.repository.MasterAgreementRepository;
import com.davivienda.factoraje.repository.PaymentPolicyCatRepository;
import com.davivienda.factoraje.repository.RoleCatRepository;
import com.davivienda.factoraje.repository.UserRepository;
import com.davivienda.factoraje.service.DisbursementPolicyService;

class OperatorChangeNoticeTest {

    private final MailNoticePublisher mailNotices = org.mockito.Mockito.mock(MailNoticePublisher.class);

    @Test
    void creatingAHolidayAlertsBankUsers() {
        BankHolidayCatRepository holidays = org.mockito.Mockito.mock(BankHolidayCatRepository.class);
        LocalDate date = LocalDate.of(2026, 12, 25);
        when(holidays.existsByHolidayDate(date)).thenReturn(false);
        when(holidays.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new HolidayServiceImpl(holidays, mailNotices).createHoliday(new HolidayDTORequest(date, "Navidad"));

        verify(mailNotices).operatorChanged("Feriados", MailNoticePublisher.NO_RECORD, "Feriado del 25/12/2026: Navidad");
    }

    @Test
    void creatingAUserAlertsBankUsers() {
        UserRepository users = org.mockito.Mockito.mock(UserRepository.class);
        EntityRepository entities = org.mockito.Mockito.mock(EntityRepository.class);
        RoleCatRepository roles = org.mockito.Mockito.mock(RoleCatRepository.class);
        CurrentUserService currentUser = org.mockito.Mockito.mock(CurrentUserService.class);
        when(currentUser.dui()).thenReturn("000000000");

        UUID entityId = UUID.randomUUID();
        UUID roleId = UUID.randomUUID();
        EntityModel entity = EntityModel.builder()
                .id(entityId)
                .name("Pagador Demo")
                .entityType(EntityTypeCat.builder().code(EntityTypeCode.PAYER).build())
                .build();
        RoleCat role = RoleCat.builder().id(roleId).name(Roles.PAYER).build();
        when(entities.findById(entityId)).thenReturn(Optional.of(entity));
        when(roles.findById(roleId)).thenReturn(Optional.of(role));
        when(users.saveAndFlush(any(UserModel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        new UserServiceImpl(users, entities, roles, currentUser, mailNotices).createUser(new UserDTORequest(
                "01234567-8", entityId, roleId, "Ana", "Pagadora", "ana@empresa.com"));

        verify(mailNotices).operatorChanged(eq("Usuarios"), eq(MailNoticePublisher.NO_RECORD),
                contains("Usuario Ana Pagadora (ana@empresa.com)"));
    }

    @Test
    void updatingAnAgreementAlertsBankUsers() {
        MasterAgreementRepository agreements = org.mockito.Mockito.mock(MasterAgreementRepository.class);
        DisbursementPolicyCatRepository disbursements = org.mockito.Mockito.mock(DisbursementPolicyCatRepository.class);
        PaymentPolicyCatRepository payments = org.mockito.Mockito.mock(PaymentPolicyCatRepository.class);
        EntityRepository entities = org.mockito.Mockito.mock(EntityRepository.class);
        UUID agreementId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        UUID disbursementId = UUID.randomUUID();
        EntityModel payer = EntityModel.builder().id(UUID.randomUUID()).name("Pagador Demo").build();
        EntityModel supplier = EntityModel.builder().id(UUID.randomUUID()).name("Proveedor Uno").build();
        when(agreements.findById(agreementId)).thenReturn(Optional.of(
                MasterAgreementModel.builder()
                        .id(agreementId)
                        .payer(payer)
                        .supplier(supplier)
                        .paymentPolicy(PaymentPolicyCat.builder().id(UUID.randomUUID()).code("P30").build())
                        .disbursementPolicy(DisbursementPolicyCat.builder().id(UUID.randomUUID()).name("T+0").build())
                        .build()));
        when(payments.findById(paymentId)).thenReturn(Optional.of(
                PaymentPolicyCat.builder().id(paymentId).code("P30").daysCount(30).build()));
        when(disbursements.findById(disbursementId)).thenReturn(Optional.of(
                DisbursementPolicyCat.builder().id(disbursementId).name("T+1").build()));
        when(agreements.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        new MasterAgreementServiceImpl(agreements, disbursements, payments, entities,
                org.mockito.Mockito.mock(BankAccountRepository.class),
                org.mockito.Mockito.mock(DisbursementPolicyService.class), mailNotices)
                .updateMasterAgreement(agreementId, new MasterAgreementDTORequest(paymentId, disbursementId));

        verify(mailNotices).operatorChanged(eq("Acuerdos"), contains("Convenio Pagador Demo – Proveedor Uno"),
                contains("política de desembolso T+1"));
    }
}
