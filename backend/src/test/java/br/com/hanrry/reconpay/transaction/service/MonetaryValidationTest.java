package br.com.hanrry.reconpay.transaction.service;

import br.com.hanrry.reconpay.transaction.dto.CreateTransactionRequestDTO;
import br.com.hanrry.reconpay.feerule.dto.FeeRuleRequestDTO;
import br.com.hanrry.reconpay.feerule.dto.UpdateFeeRuleRequestDTO;
import br.com.hanrry.reconpay.externalsettlement.service.SettlementCsvParser;
import br.com.hanrry.reconpay.exception.SettlementImportValidationException;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

class MonetaryValidationTest {
    @ParameterizedTest @ValueSource(strings = {"1.001", "100000000000000000.00", "1E+19"})
    void rejectsJsonMoneyOutsideDatabasePrecision(String raw) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertThat(validator.validate(new CreateTransactionRequestDTO("ref", new BigDecimal(raw), PaymentMethod.PIX, 1, LocalDate.of(2026, 1, 1)))).isNotEmpty();
            assertThat(validator.validate(new FeeRuleRequestDTO(PaymentMethod.PIX, 1, BigDecimal.ZERO, new BigDecimal(raw)))).isNotEmpty();
            assertThat(validator.validate(new UpdateFeeRuleRequestDTO(null, null, null, new BigDecimal(raw)))).isNotEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"100.0001", "1.00001"})
    void rejectsOutOfRangeOrOverPreciseFeePercentages(String raw) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(new FeeRuleRequestDTO(PaymentMethod.PIX, 1, new BigDecimal(raw), BigDecimal.ZERO))).isNotEmpty();
            assertThat(factory.getValidator().validate(new UpdateFeeRuleRequestDTO(null, null, new BigDecimal(raw), null))).isNotEmpty();
        }
    }

    @ParameterizedTest @ValueSource(strings = {"1.001", "100000000000000000.00", "1E+19"})
    void rejectsCsvMoneyOutsideDatabasePrecision(String raw) {
        var csv = "externalReference,amount,netAmount,paymentMethod,installments,status,settlementDate\nref," + raw + ",1.00,PIX,1,APPROVED,2026-01-01\n";
        assertThatThrownBy(() -> new SettlementCsvParser(Clock.systemUTC()).parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(SettlementImportValidationException.class);
    }
}
