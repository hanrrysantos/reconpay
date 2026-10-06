package br.com.hanrry.reconpay.reconciliation.validation;

import br.com.hanrry.reconpay.reconciliation.dto.UpdateDiscrepancyStatusRequestDTO;
import br.com.hanrry.reconpay.reconciliation.enums.DiscrepancyStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DiscrepancyStatusRequestValidatorTest {

    private static Set<ConstraintViolation<UpdateDiscrepancyStatusRequestDTO>> validate(
            UpdateDiscrepancyStatusRequestDTO request) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();
            return validator.validate(request);
        }
    }

    @Test
    void rejectsNoteLongerThan500Characters() {
        var violations = validate(new UpdateDiscrepancyStatusRequestDTO(
                DiscrepancyStatus.ACCEPTED, "x".repeat(501), null));

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("note");
    }

    @Test
    void rejectsAdjustedWithNullCorrectionAmount() {
        var violations = validate(new UpdateDiscrepancyStatusRequestDTO(
                DiscrepancyStatus.ADJUSTED, null, null));

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("");
    }

    @Test
    void rejectsAdjustedWithZeroCorrectionAmount() {
        var violations = validate(new UpdateDiscrepancyStatusRequestDTO(
                DiscrepancyStatus.ADJUSTED, null, BigDecimal.ZERO));

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("");
    }

    @Test
    void rejectsAdjustedWithCorrectionAmountScaleAboveTwo() {
        var violations = validate(new UpdateDiscrepancyStatusRequestDTO(
                DiscrepancyStatus.ADJUSTED, null, new BigDecimal("1.001")));

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("correctionAmount");
    }

    @Test
    void rejectsAdjustedWithCorrectionAmountExceedingIntegerDigits() {
        var violations = validate(new UpdateDiscrepancyStatusRequestDTO(
                DiscrepancyStatus.ADJUSTED, null, new BigDecimal("100000000000000000.00")));

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("correctionAmount");
    }

    @Test
    void rejectsAcceptedWithNonNullCorrectionAmount() {
        var violations = validate(new UpdateDiscrepancyStatusRequestDTO(
                DiscrepancyStatus.ACCEPTED, null, new BigDecimal("1.00")));

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("");
    }

    @Test
    void rejectsWrittenOffWithNonNullCorrectionAmount() {
        var violations = validate(new UpdateDiscrepancyStatusRequestDTO(
                DiscrepancyStatus.WRITTEN_OFF, null, new BigDecimal("1.00")));

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("");
    }

    @Test
    void rejectsOpenWithNonNullCorrectionAmount() {
        var violations = validate(new UpdateDiscrepancyStatusRequestDTO(
                DiscrepancyStatus.OPEN, null, new BigDecimal("1.00")));

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactly("");
    }

    @Test
    void acceptsAdjustedWithNegativeCorrectionAmountAndNullNote() {
        var violations = validate(new UpdateDiscrepancyStatusRequestDTO(
                DiscrepancyStatus.ADJUSTED, null, new BigDecimal("-1.50")));

        assertThat(violations).isEmpty();
    }

    @Test
    void acceptsAcceptedWithNullCorrectionAmountAndMaxLengthNote() {
        var violations = validate(new UpdateDiscrepancyStatusRequestDTO(
                DiscrepancyStatus.ACCEPTED, "n".repeat(500), null));

        assertThat(violations).isEmpty();
    }
}
