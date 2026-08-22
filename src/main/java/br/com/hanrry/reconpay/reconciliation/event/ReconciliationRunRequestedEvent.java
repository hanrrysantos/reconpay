package br.com.hanrry.reconpay.reconciliation.event;

import java.util.UUID;

/**
 * Carries only the run id. The worker reloads the run in its own transaction,
 * so it can never act on state that the requesting transaction rolled back.
 */
public record ReconciliationRunRequestedEvent(UUID runId) {
}
