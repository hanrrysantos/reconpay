package br.com.hanrry.reconpay.demo.service;

import java.util.UUID;

/* What a fresh demo needs after its scenario commits: who it is and which run to finish. */
public record DemoAccount(UUID userId, String email, UUID runId) {
}
