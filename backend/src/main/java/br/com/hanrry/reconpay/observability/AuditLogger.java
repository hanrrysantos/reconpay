package br.com.hanrry.reconpay.observability;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Records who performed a state-changing action. Reconciliation is a financial
 * control, so "who changed this transaction's status" has to be answerable.
 */
@Slf4j(topic = "reconpay.audit")
@Component
public class AuditLogger {

    public void record(String action, String resource, Object resourceId) {
        record(action, resource, resourceId, null);
    }

    public void record(String action, String resource, Object resourceId, String detail) {
        String actor = currentActor();
        var context = MDC.getCopyOfContextMap();
        Runnable output = () -> {
            var previous = MDC.getCopyOfContextMap();
            try {
                if (context == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(context);
                }
                log.info("audit | actor={} | action={} | resource={} | resourceId={} | detail={}",
                        actor, action, resource, resourceId, detail);
            } finally {
                if (previous == null) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(previous);
                }
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            output.run();
                        }
                    });
        } else {
            output.run();
        }
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "anonymous" : authentication.getName();
    }
}
