package br.com.hanrry.reconpay.demo.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@ConfigurationProperties(prefix = "reconpay.demo")
@Validated
public record DemoProperties(

        /* How long a demo account lives, and the longest its token may last. */
        @NotNull @DefaultValue("24h") Duration ttl,

        /* Demo accounts allowed to exist unexpired at the same time. */
        @Min(1) @DefaultValue("100") int maxActive,

        /* Successful creations allowed per client address in a sliding hour. */
        @Min(1) @DefaultValue("2") int perIpPerHour,

        /* Merchants a demo account may hold, the scenario merchant included. */
        @Min(1) @DefaultValue("2") int maxMerchants,

        /* CSV imports a demo account may make on its own. */
        @Min(0) @DefaultValue("10") int maxImports,

        /* Pause between cleanup sweeps of expired demo accounts. */
        @Min(1) @DefaultValue("900000") long cleanupDelayMs,

        /* Longest a creation waits for a run the background dispatcher claimed. */
        @NotNull @DefaultValue("30s") Duration runWaitTimeout
) {
}
