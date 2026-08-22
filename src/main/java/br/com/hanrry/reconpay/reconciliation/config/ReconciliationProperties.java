package br.com.hanrry.reconpay.reconciliation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "reconpay.reconciliation")
public record ReconciliationProperties(

        /*
         * Absolute difference tolerated before an amount counts as divergent.
         * Acquirers round per installment, so an exact comparison floods the
         * divergent bucket with one-cent noise.
         */
        @DefaultValue("0.00") BigDecimal amountTolerance,

        /*
         * Days a settlement may land after the requested window closes. A sale on
         * the last day of the period settles in the next one, and without this the
         * same event surfaces as MISSING_SETTLEMENT in one run and
         * ORPHAN_SETTLEMENT in the next, never reconciling.
         */
        @DefaultValue("5") int settlementLagDays,

        /* Widest window a single run may cover. */
        @DefaultValue("366") int maxWindowDays
) {
}
