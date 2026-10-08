package br.com.hanrry.reconpay.reconciliation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "reconpay.reconciliation")
@Validated
public record ReconciliationProperties(

        /*
         * Absolute difference tolerated before an amount counts as divergent.
         * Acquirers round per installment, so an exact comparison floods the
         * divergent bucket with one-cent noise.
         */
        @NotNull
        @DecimalMin("0.00")
        @DefaultValue("0.00") BigDecimal amountTolerance,

        /*
         * Days a settlement may land after the requested window closes. A sale on
         * the last day of the period settles in the next one, and without this the
         * same event surfaces as MISSING_SETTLEMENT in one run and
         * ORPHAN_SETTLEMENT in the next, never reconciling.
         */
        @Min(0) @DefaultValue("5") int settlementLagDays,

        /* Widest window a single run may cover. */
        @Min(1) @DefaultValue("366") int maxWindowDays,

        /*
         * Whether runs execute on a background pool. Turning it off keeps the
         * work on the dispatcher's thread. HTTP requests still return PENDING.
         */
        @DefaultValue("true") boolean async,

        /* Concurrent runs allowed across all merchants. */
        @Min(1) @DefaultValue("2") int workers,

        /* Requests allowed to wait for a free worker before being rejected. */
        @Min(0) @DefaultValue("50") int queueCapacity
) {
}
