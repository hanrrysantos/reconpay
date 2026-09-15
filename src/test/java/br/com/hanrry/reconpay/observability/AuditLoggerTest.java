package br.com.hanrry.reconpay.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class AuditLoggerTest {
    private final AuditLogger audit = new AuditLogger();
    private final Logger logger = (Logger) LoggerFactory.getLogger("reconpay.audit");
    private final ListAppender<ILoggingEvent> events = new ListAppender<>() {
        @Override protected void append(ILoggingEvent event) {
            event.prepareForDeferredProcessing();
            super.append(event);
        }
    };
    private final TransactionTemplate transaction = new TransactionTemplate(new AbstractPlatformTransactionManager() {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object tx, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
    });

    @BeforeEach void attach() { events.start(); logger.addAppender(events); }
    @AfterEach void detach() { logger.detachAppender(events); events.stop(); MDC.clear(); SecurityContextHolder.clearContext(); }

    @Test void commitLogsOnlyAfterCommitWithOriginalActorAndContext() {
        transaction.executeWithoutResult(tx -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("analyst@example.com", ""));
            MDC.put("requestId", "original-request");
            audit.record("UPDATED", "transaction", "42", "APPROVED -> REFUNDED");
            assertThat(events.list).isEmpty();
            MDC.put("requestId", "later-context");
            SecurityContextHolder.clearContext();
        });
        assertThat(events.list).hasSize(1);
        assertThat(events.list.getFirst().getFormattedMessage()).contains("analyst@example.com", "APPROVED -> REFUNDED");
        assertThat(events.list.getFirst().getMDCPropertyMap()).containsEntry("requestId", "original-request");
        assertThat(MDC.get("requestId")).isEqualTo("later-context");
    }

    @Test void rollbackDoesNotLogSuccess() {
        transaction.executeWithoutResult(tx -> {
            audit.record("CREATED", "transaction", "42");
            tx.setRollbackOnly();
        });
        assertThat(events.list).isEmpty();
    }

    @Test void outsideTransactionLogsImmediately() {
        audit.record("CREATED", "transaction", "42");
        assertThat(events.list).hasSize(1);
        assertThat(events.list.getFirst().getFormattedMessage()).contains("action=CREATED", "resourceId=42");
    }
}
