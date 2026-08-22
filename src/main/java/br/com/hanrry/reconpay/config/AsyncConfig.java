package br.com.hanrry.reconpay.config;

import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * A bounded pool with a bounded queue, so a burst of reconciliation requests
     * is rejected at submission rather than accumulating until the heap gives
     * out. Set {@code reconpay.reconciliation.async=false} to run the work
     * inline, which is what the tests do to stay deterministic.
     */
    @Bean("reconciliationExecutor")
    public Executor reconciliationExecutor(ReconciliationProperties properties) {
        if (!properties.async()) {
            return new SyncTaskExecutor();
        }

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.workers());
        executor.setMaxPoolSize(properties.workers());
        executor.setQueueCapacity(properties.queueCapacity());
        executor.setThreadNamePrefix("reconciliation-");
        executor.setTaskDecorator(mdcPropagatingDecorator());
        executor.initialize();

        return executor;
    }

    /* Without this the worker's log lines carry no requestId and cannot be tied
     * back to the request that triggered the run. */
    private TaskDecorator mdcPropagatingDecorator() {
        return runnable -> {
            Map<String, String> context = MDC.getCopyOfContextMap();

            return () -> {
                if (context != null) {
                    MDC.setContextMap(context);
                }
                try {
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        };
    }
}
