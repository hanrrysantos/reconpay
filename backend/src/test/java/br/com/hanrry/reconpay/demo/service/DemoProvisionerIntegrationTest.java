package br.com.hanrry.reconpay.demo.service;

import br.com.hanrry.reconpay.auth.email.CapturingEmailSender;
import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.auth.enums.UserRole;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.bankstatement.service.BankStatementService;
import br.com.hanrry.reconpay.base.AbstractIntegrationTest;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.reconciliation.entity.ReconciliationRunEntity;
import br.com.hanrry.reconpay.reconciliation.repository.IReconciliationRunRepository;
import br.com.hanrry.reconpay.shared.CnpjRules;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.sql.Date;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DemoProvisionerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private DemoProvisioner provisioner;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IReconciliationRunRepository runRepository;

    @Autowired
    private CapturingEmailSender emailSender;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private IMerchantRepository merchantRepository;

    @MockitoSpyBean
    private BankStatementService bankStatementService;

    @Test
    void provisionWritesAnActiveOperatorDemoThatExpiresInTwentyFourHoursWithoutEmail() {
        emailSender.clear();
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);

        DemoAccount account = provisioner.provision(now);

        UserEntity user = userRepository.findById(account.userId()).orElseThrow();
        assertThat(user.getEmail()).isEqualTo(account.email())
                .matches("demo-[0-9a-f]{12}@demo\\.reconpay\\.invalid");
        assertThat(user.getName()).isEqualTo("Visitante");
        assertThat(user.getRole()).isEqualTo(UserRole.OPERATOR);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getDemoExpiresAt()).isEqualTo(now.plus(Duration.ofHours(24)));
        assertThat(emailSender.lastEmail()).isNull();
    }

    @Test
    void provisionGrantsOneMerchantWithTheScenarioRulesBatchesDatesAndRun() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        LocalDate today = LocalDate.ofInstant(now, ZoneOffset.UTC);

        DemoAccount account = provisioner.provision(now);

        List<UUID> merchantIds = jdbc.queryForList(
                "SELECT merchant_id FROM user_merchants WHERE user_id = ?", UUID.class, account.userId());
        assertThat(merchantIds).hasSize(1);
        UUID merchantId = merchantIds.get(0);
        String document = jdbc.queryForObject("SELECT document FROM merchants WHERE id = ?", String.class, merchantId);
        assertThat(CnpjRules.isValid(document)).isTrue();

        assertThat(jdbc.queryForList(
                "SELECT payment_method FROM fee_rules WHERE merchant_id = ? AND active", String.class, merchantId))
                .containsExactlyInAnyOrder("CREDIT_CARD", "DEBIT_CARD", "PIX", "BOLETO");
        for (String batches : List.of("transaction_imports", "settlement_imports", "bank_statement_imports")) {
            assertThat(count("SELECT count(*) FROM " + batches + " WHERE merchant_id = ?", merchantId))
                    .as(batches).isEqualTo(1L);
        }

        for (String dates : List.of(
                "SELECT min(transaction_date) AS low, max(transaction_date) AS high FROM internal_transactions",
                "SELECT min(settlement_date) AS low, max(settlement_date) AS high FROM external_settlements",
                "SELECT min(movement_date) AS low, max(movement_date) AS high FROM bank_statement_lines")) {
            Map<String, Object> range = jdbc.queryForMap(dates + " WHERE merchant_id = ?", merchantId);
            assertThat(((Date) range.get("low")).toLocalDate()).as(dates).isAfterOrEqualTo(today.minusDays(60));
            assertThat(((Date) range.get("high")).toLocalDate()).as(dates).isBeforeOrEqualTo(today.minusDays(1));
        }

        ReconciliationRunEntity run = runRepository.findById(account.runId()).orElseThrow();
        assertThat(run.getMerchant().getId()).isEqualTo(merchantId);
        assertThat(run.getFromDate()).isEqualTo(today.minusDays(60));
        assertThat(run.getToDate()).isEqualTo(today.minusDays(1));
    }

    @Test
    void provisionDrawsAnotherDocumentWhenTheFirstOneIsAlreadyTaken() {
        doReturn(true, false).when(merchantRepository).existsByDocumentDigits(anyString());

        DemoAccount account = provisioner.provision(Instant.now());

        ArgumentCaptor<String> tried = ArgumentCaptor.forClass(String.class);
        verify(merchantRepository, times(2)).existsByDocumentDigits(tried.capture());
        String document = jdbc.queryForObject("""
                SELECT m.document FROM merchants m JOIN user_merchants um ON um.merchant_id = m.id
                WHERE um.user_id = ?
                """, String.class, account.userId());
        assertThat(tried.getAllValues().get(0)).isNotEqualTo(tried.getAllValues().get(1));
        assertThat(document).isEqualTo(tried.getAllValues().get(1));
        assertThat(CnpjRules.isValid(document)).isTrue();
    }

    @Test
    void failureHalfwayLeavesNoUserNorMerchant() {
        doThrow(new IllegalStateException("extrato indisponível"))
                .when(bankStatementService).importCsv(any(), any());
        long users = userRepository.count();
        long merchants = merchantRepository.count();
        long demoMerchants = count("SELECT count(*) FROM merchants WHERE name = ?", "Loja Demonstração");

        assertThatThrownBy(() -> provisioner.provision(Instant.now()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("extrato indisponível");

        assertThat(userRepository.count()).isEqualTo(users);
        assertThat(merchantRepository.count()).isEqualTo(merchants);
        assertThat(count("SELECT count(*) FROM merchants WHERE name = ?", "Loja Demonstração"))
                .isEqualTo(demoMerchants);
        verify(bankStatementService).importCsv(any(), any());
    }

    private long count(String sql, Object argument) {
        return jdbc.queryForObject(sql, Long.class, argument);
    }
}
