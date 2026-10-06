package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.davivienda.factoraje.domain.catalogs.EntityTypeCat;
import com.davivienda.factoraje.domain.constants.EntityTypeCode;
import com.davivienda.factoraje.domain.entities.BankAccountModel;
import com.davivienda.factoraje.domain.entities.CreditFacilityModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.ProductPricingTermModel;
import com.davivienda.factoraje.domain.enums.CalculationBaseEnum;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;
import com.davivienda.factoraje.dto.entity.PayerRegistrationDTORequest;
import com.davivienda.factoraje.dto.entity.PayerSummaryDTOResponse;
import com.davivienda.factoraje.dto.entity.PayerUpdateDTORequest;
import com.davivienda.factoraje.infrastructure.exception.ResourceAlreadyExistsException;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.infrastructure.security.CurrentUserService;
import com.davivienda.factoraje.repository.BankAccountRepository;
import com.davivienda.factoraje.repository.CreditFacilityHistoryRepository;
import com.davivienda.factoraje.repository.CreditFacilityRepository;
import com.davivienda.factoraje.repository.EntityRepository;
import com.davivienda.factoraje.repository.EntityTypeCatRepository;
import com.davivienda.factoraje.repository.ProductPricingTermRepository;
import com.davivienda.factoraje.repository.RoleCatRepository;

@ExtendWith(MockitoExtension.class)
class EntityServiceImplTest {

    private static final String NIT = "0614-010190-101-1";
    private static final String ACCOUNT = "0012-3456-789";

    @Mock
    private EntityRepository entityRepository;
    @Mock
    private EntityTypeCatRepository entityTypeCatRepository;
    @Mock
    private CreditFacilityRepository creditFacilityRepository;
    @Mock
    private ProductPricingTermRepository productPricingTermRepository;
    @Mock
    private CreditFacilityHistoryRepository creditFacilityHistoryRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private RoleCatRepository roleCatRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private MailNoticePublisher mailNotices;

    @InjectMocks
    private EntityServiceImpl service;

    @Test
    void registerPayerSavesNormalizedMainBankAccount() {
        when(entityTypeCatRepository.findByCode(EntityTypeCode.PAYER))
                .thenReturn(Optional.of(EntityTypeCat.builder().code(EntityTypeCode.PAYER).build()));
        when(entityRepository.saveAndFlush(any(EntityModel.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bankAccountRepository.saveAndFlush(any(BankAccountModel.class))).thenAnswer(inv -> inv.getArgument(0));
        when(creditFacilityRepository.saveAndFlush(any(CreditFacilityModel.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productPricingTermRepository.saveAndFlush(any(ProductPricingTermModel.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PayerSummaryDTOResponse response = service.registerPayer(registration(ACCOUNT));

        assertThat(response.status()).isEqualTo(GeneralStatusEnum.ACTIVE);
        ArgumentCaptor<BankAccountModel> captor = ArgumentCaptor.forClass(BankAccountModel.class);
        verify(bankAccountRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getAccountNumber()).isEqualTo("00123456789");
        assertThat(captor.getValue().getIsMain()).isTrue();
        assertThat(captor.getValue().getStatus()).isEqualTo(GeneralStatusEnum.ACTIVE);
        assertThat(response.accountNumber()).isEqualTo("00123456789");
    }

    @Test
    void registerPayerIsRejectedWhenAccountBelongsToAnotherEntity() {
        when(bankAccountRepository.existsByAccountNumber("00123456789")).thenReturn(true);

        assertThatThrownBy(() -> service.registerPayer(registration(ACCOUNT)))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("00123456789");

        verify(entityRepository, never()).saveAndFlush(any());
    }

    @Test
    void registerPayerRequiresAccountNumber() {
        assertThatThrownBy(() -> service.registerPayer(registration("  ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cuenta bancaria es obligatoria");
    }

    @Test
    void updatePayerCannotRegisterAnAccountWhenNoneExists() {
        UUID payerId = UUID.randomUUID();
        CreditFacilityModel facility = CreditFacilityModel.builder().id(UUID.randomUUID()).build();
        when(entityRepository.findById(payerId)).thenReturn(Optional.of(payer(payerId)));
        when(creditFacilityRepository.findByPayerIdForUpdate(payerId)).thenReturn(Optional.of(facility));
        when(productPricingTermRepository.findByCreditFacilityId(facility.getId())).thenReturn(Optional.empty());
        when(bankAccountRepository.findFirstByEntityModelIdAndIsMainTrue(payerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updatePayer(payerId, update(ACCOUNT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no tiene una cuenta bancaria principal");

        verify(entityRepository, never()).saveAndFlush(any());
    }

    @Test
    void updatePayerReplacesExistingMainAccount() {
        UUID payerId = UUID.randomUUID();
        CreditFacilityModel facility = CreditFacilityModel.builder().id(UUID.randomUUID()).build();
        BankAccountModel account = BankAccountModel.builder()
                .id(UUID.randomUUID()).accountNumber("111").isMain(true).status(GeneralStatusEnum.ACTIVE).build();
        when(entityRepository.findById(payerId)).thenReturn(Optional.of(payer(payerId)));
        when(creditFacilityRepository.findByPayerIdForUpdate(payerId)).thenReturn(Optional.of(facility));
        when(productPricingTermRepository.findByCreditFacilityId(facility.getId())).thenReturn(Optional.empty());
        when(bankAccountRepository.findFirstByEntityModelIdAndIsMainTrue(payerId)).thenReturn(Optional.of(account));
        when(entityRepository.saveAndFlush(any(EntityModel.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bankAccountRepository.saveAndFlush(any(BankAccountModel.class))).thenAnswer(inv -> inv.getArgument(0));
        when(creditFacilityRepository.saveAndFlush(any(CreditFacilityModel.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productPricingTermRepository.saveAndFlush(any(ProductPricingTermModel.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PayerSummaryDTOResponse response = service.updatePayer(payerId, update(ACCOUNT));

        assertThat(account.getAccountNumber()).isEqualTo("00123456789");
        assertThat(response.accountNumber()).isEqualTo("00123456789");
    }

    private static EntityModel payer(UUID id) {
        return EntityModel.builder()
                .id(id)
                .name("Pagador Uno")
                .entityType(EntityTypeCat.builder().code(EntityTypeCode.PAYER).build())
                .status(GeneralStatusEnum.ACTIVE)
                .build();
    }

    private static PayerRegistrationDTORequest registration(String accountNumber) {
        return new PayerRegistrationDTORequest(
                "Pagador Uno", NIT, accountNumber, "CF-0001", new BigDecimal("100000"),
                new BigDecimal("0.15"), new BigDecimal("0.01"), CalculationBaseEnum.COMERCIAL_360,
                new BigDecimal("0.80"), null, null);
    }

    private static PayerUpdateDTORequest update(String accountNumber) {
        return new PayerUpdateDTORequest(
                accountNumber, new BigDecimal("0.15"), new BigDecimal("0.01"), CalculationBaseEnum.COMERCIAL_360,
                GeneralStatusEnum.ACTIVE, new BigDecimal("0.80"));
    }
}
