package br.com.hanrry.reconpay.config;

import br.com.hanrry.reconpay.reconciliation.config.ReconciliationProperties;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;

@Configuration
@EnableScheduling
public class AsyncConfig {

    /**
     * A bounded pool with a bounded queue, so a burst of reconciliation requests
     * is rejected at submission and returned to persisted PENDING state by the
     * dispatcher. Set {@code reconpay.reconciliation.async=false} to run the
     * work on the dispatcher's thread.
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

    /* Preserve any dispatch context; the worker also correlates logs by runId. */
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
