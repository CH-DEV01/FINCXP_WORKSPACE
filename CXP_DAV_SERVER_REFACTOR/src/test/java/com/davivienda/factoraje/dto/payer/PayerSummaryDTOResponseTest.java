package com.davivienda.factoraje.dto.payer;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.davivienda.factoraje.domain.entities.CreditFacilityModel;
import com.davivienda.factoraje.domain.entities.EntityModel;
import com.davivienda.factoraje.domain.enums.GeneralStatusEnum;

class PayerSummaryDTOResponseTest {

    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("0.90");

    private final EntityModel payer = EntityModel.builder()
            .name("AEROMAN S.A. DE C.V").nit("06142711011200").code("P-01").build();

    @Test
    void uploadLimitUsesTheLineThreshold() {
        PayerSummaryDTOResponse.CreditLine line = PayerSummaryDTOResponse
                .of(payer, facility("15000", "9500", "0.80"), DEFAULT_THRESHOLD).creditLine();

        assertThat(line.utilizationPercentage()).isEqualByComparingTo("63.33");
        assertThat(line.thresholdPercentage()).isEqualByComparingTo("80");
        assertThat(line.uploadLimitAmount()).isEqualByComparingTo("12000");
        assertThat(line.availableToUpload()).isEqualByComparingTo("2500");
        assertThat(line.availableAmount()).isEqualByComparingTo("5500");
    }

    @Test
    void availableToUploadIsNeverNegativeAndFallsBackToTheDefaultThreshold() {
        PayerSummaryDTOResponse.CreditLine line = PayerSummaryDTOResponse
                .of(payer, facility("10000", "9500", null), DEFAULT_THRESHOLD).creditLine();

        assertThat(line.thresholdPercentage()).isEqualByComparingTo("90");
        assertThat(line.uploadLimitAmount()).isEqualByComparingTo("9000");
        assertThat(line.availableToUpload()).isEqualByComparingTo("0");
    }

    @Test
    void payerWithoutCreditLineHasNoCreditLine() {
        PayerSummaryDTOResponse summary = PayerSummaryDTOResponse.of(payer, null, DEFAULT_THRESHOLD);

        assertThat(summary.nit()).isEqualTo("06142711011200");
        assertThat(summary.creditLine()).isNull();
    }

    private static CreditFacilityModel facility(String limit, String inUse, String threshold) {
        return CreditFacilityModel.builder()
                .status(GeneralStatusEnum.ACTIVE)
                .facilityLimitAmount(new BigDecimal(limit))
                .amountInUse(new BigDecimal(inUse))
                .warningThresholdPercentage(threshold != null ? new BigDecimal(threshold) : null)
                .build();
    }
}
