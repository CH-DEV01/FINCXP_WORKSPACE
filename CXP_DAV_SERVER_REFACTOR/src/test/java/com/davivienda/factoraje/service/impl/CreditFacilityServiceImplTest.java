package com.davivienda.factoraje.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.davivienda.factoraje.domain.entities.CreditFacilityHistoryModel;
import com.davivienda.factoraje.domain.entities.CreditFacilityModel;
import com.davivienda.factoraje.domain.entities.DisbursementBatchModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.entities.UserModel;
import com.davivienda.factoraje.domain.enums.DisbursementBatchStatusEnum;
import com.davivienda.factoraje.domain.enums.RepaymentTypeEnum;
import com.davivienda.factoraje.dto.credit_facility.RestoreCreditFacilityDTORequest;
import com.davivienda.factoraje.infrastructure.config.SystemParameters;
import com.davivienda.factoraje.infrastructure.exception.ResourceAlreadyExistsException;
import com.davivienda.factoraje.infrastructure.mail.MailNoticePublisher;
import com.davivienda.factoraje.repository.CreditFacilityHistoryRepository;
import com.davivienda.factoraje.repository.CreditFacilityRepository;
import com.davivienda.factoraje.repository.DisbursementBatchRepository;
import com.davivienda.factoraje.repository.ProductPricingTermRepository;
import com.davivienda.factoraje.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CreditFacilityServiceImplTest {

    private static final String DUI = "012345678";

    @Mock
    private CreditFacilityRepository creditFacilityRepository;
    @Mock
    private CreditFacilityHistoryRepository creditFacilityHistoryRepository;
    @Mock
    private DisbursementBatchRepository disbursementBatchRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ProductPricingTermRepository productPricingTermRepository;
    @Mock
    private SystemParameters systemParameters;
    @Mock
    private MailNoticePublisher mailNotices;

    @InjectMocks
    private CreditFacilityServiceImpl service;

    private final EntityModel payer = EntityModel.builder().id(UUID.randomUUID()).name("Pagador Demo").build();
    private final UserModel admin = UserModel.builder().id(UUID.randomUUID()).dui(DUI).build();
    private CreditFacilityModel facility;

    @BeforeEach
    void setUp() {
        facility = CreditFacilityModel.builder()
                .id(UUID.randomUUID())
                .payer(payer)
                .facilityLimitAmount(new BigDecimal("100000"))
                .amountInUse(new BigDecimal("50000"))
                .build();
        lenient().when(systemParameters.getDecimal(any())).thenReturn(new BigDecimal("0.80"));
        lenient().when(userRepository.findByDui(DUI)).thenReturn(Optional.of(admin));
        lenient().when(creditFacilityRepository.findByIdForUpdate(facility.getId())).thenReturn(Optional.of(facility));
    }

    @Test
    void partialRepaymentRestoresTheNominalAmountOfTheSelectedBatch() {
        DisbursementBatchModel batch = batch(payer, DisbursementBatchStatusEnum.SETTLED);
        when(disbursementBatchRepository.findByIdForUpdate(batch.getId())).thenReturn(Optional.of(batch));
        when(disbursementBatchRepository.sumNominalAmount(batch.getId())).thenReturn(new BigDecimal("12000.50"));

        service.restoreCreditFacility(partial(batch.getId(), new BigDecimal("1")), DUI);

        assertThat(facility.getAmountInUse()).isEqualByComparingTo("37999.50");
        ArgumentCaptor<CreditFacilityHistoryModel> captor = ArgumentCaptor.forClass(CreditFacilityHistoryModel.class);
        verify(creditFacilityHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("12000.50");
        assertThat(captor.getValue().getDisbursementBatch()).isSameAs(batch);
        assertThat(captor.getValue().getRepaymentType()).isEqualTo(RepaymentTypeEnum.PARTIAL);
    }

    @Test
    void partialRepaymentRequiresABatch() {
        assertThatThrownBy(() -> service.restoreCreditFacility(partial(null, null), DUI))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Seleccione el lote");
        verify(creditFacilityRepository, never()).save(any());
    }

    @Test
    void aBatchCannotBeRepaidTwice() {
        DisbursementBatchModel batch = batch(payer, DisbursementBatchStatusEnum.SETTLED);
        when(disbursementBatchRepository.findByIdForUpdate(batch.getId())).thenReturn(Optional.of(batch));
        when(creditFacilityHistoryRepository.existsByDisbursementBatchId(batch.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.restoreCreditFacility(partial(batch.getId(), null), DUI))
                .isInstanceOf(ResourceAlreadyExistsException.class)
                .hasMessageContaining("ya fue abonado");
        verify(creditFacilityRepository, never()).save(any());
    }

    @Test
    void rejectsABatchOfAnotherPayerOrNotYetDisbursed() {
        DisbursementBatchModel foreign = batch(EntityModel.builder().id(UUID.randomUUID()).build(),
                DisbursementBatchStatusEnum.SETTLED);
        DisbursementBatchModel pending = batch(payer, DisbursementBatchStatusEnum.CREATED);
        when(disbursementBatchRepository.findByIdForUpdate(foreign.getId())).thenReturn(Optional.of(foreign));
        when(disbursementBatchRepository.findByIdForUpdate(pending.getId())).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.restoreCreditFacility(partial(foreign.getId(), null), DUI))
                .hasMessageContaining("no pertenece al pagador");
        assertThatThrownBy(() -> service.restoreCreditFacility(partial(pending.getId(), null), DUI))
                .hasMessageContaining("aún no está desembolsado");
        verify(creditFacilityRepository, never()).save(any());
    }

    @Test
    void rejectsABatchLargerThanTheCurrentConsumption() {
        DisbursementBatchModel batch = batch(payer, DisbursementBatchStatusEnum.SETTLED);
        when(disbursementBatchRepository.findByIdForUpdate(batch.getId())).thenReturn(Optional.of(batch));
        when(disbursementBatchRepository.sumNominalAmount(batch.getId())).thenReturn(new BigDecimal("60000"));

        assertThatThrownBy(() -> service.restoreCreditFacility(partial(batch.getId(), null), DUI))
                .hasMessageContaining("supera el consumo actual");
        assertThat(facility.getAmountInUse()).isEqualByComparingTo("50000");
    }

    @Test
    void fullLiquidationUsesTheRequestedAmountWithoutBatch() {
        service.restoreCreditFacility(new RestoreCreditFacilityDTORequest(
                facility.getId(), new BigDecimal("50000"), "REF-1", RepaymentTypeEnum.FULL, null), DUI);

        assertThat(facility.getAmountInUse()).isEqualByComparingTo("0");
        ArgumentCaptor<CreditFacilityHistoryModel> captor = ArgumentCaptor.forClass(CreditFacilityHistoryModel.class);
        verify(creditFacilityHistoryRepository).save(captor.capture());
        assertThat(captor.getValue().getDisbursementBatch()).isNull();
        verify(disbursementBatchRepository, never()).findByIdForUpdate(any());
    }

    private RestoreCreditFacilityDTORequest partial(UUID batchId, BigDecimal amount) {
        return new RestoreCreditFacilityDTORequest(facility.getId(), amount, "REF-1", RepaymentTypeEnum.PARTIAL, batchId);
    }

    private static DisbursementBatchModel batch(EntityModel owner, DisbursementBatchStatusEnum status) {
        return DisbursementBatchModel.builder()
                .id(UUID.randomUUID())
                .batchNumber("DSB-20261013-ABC123")
                .payer(owner)
                .status(status)
                .build();
    }
}
