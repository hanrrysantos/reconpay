package br.com.hanrry.reconpay.observability;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Records who performed a state-changing action. Reconciliation is a financial
 * control, so "who changed this transaction's status" has to be answerable.
 */
@Slf4j(topic = "reconpay.audit")
@Component
public class AuditLogger {

    public void record(String action, String resource, Object resourceId) {
        log.info("audit | actor={} | action={} | resource={} | resourceId={}",
                currentActor(), action, resource, resourceId);
    }

    public void record(String action, String resource, Object resourceId, String detail) {
        log.info("audit | actor={} | action={} | resource={} | resourceId={} | detail={}",
                currentActor(), action, resource, resourceId, detail);
    }

    private String currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? "anonymous" : authentication.getName();
    }
}
