package br.com.hanrry.reconpay.demo.service;

import br.com.hanrry.reconpay.bankstatement.service.BankStatementCsvParser;
import br.com.hanrry.reconpay.externalsettlement.enums.SettlementLayout;
import br.com.hanrry.reconpay.feerule.dto.FeeRuleRequestDTO;
import br.com.hanrry.reconpay.feerule.entity.FeeRuleEntity;
import br.com.hanrry.reconpay.feerule.service.ExpectedNetAmountCalculator;
import br.com.hanrry.reconpay.shared.enums.PaymentMethod;
import br.com.hanrry.reconpay.transaction.enums.TransactionStatus;
import br.com.hanrry.reconpay.transaction.service.TransactionCsvParser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static br.com.hanrry.reconpay.shared.enums.PaymentMethod.BOLETO;
import static br.com.hanrry.reconpay.shared.enums.PaymentMethod.CREDIT_CARD;
import static br.com.hanrry.reconpay.shared.enums.PaymentMethod.DEBIT_CARD;
import static br.com.hanrry.reconpay.shared.enums.PaymentMethod.PIX;

/*
 * The fixed demo scenario as plain data: fee rules and the three CSVs a visitor would import.
 * Nothing here is random, so every demo built on the same day is identical; only the dates move
 * with the day. A few fixed rows break the happy path on purpose so the reconciliation shows
 * every divergence the demo promises.
 */
public record DemoScenario(
        LocalDate fromDate,
        LocalDate toDate,
        List<FeeRuleRequestDTO> feeRules,
        String transactionsCsv,
        String settlementsCsv,
        String bankStatementCsv
) {

    private static final int WINDOW_DAYS = 60;
    private static final int TRANSACTIONS = 200;

    private static final List<FeeRuleRequestDTO> FEE_RULES = List.of(
            new FeeRuleRequestDTO(CREDIT_CARD, 1, new BigDecimal("2.9900"), new BigDecimal("0.00")),
            new FeeRuleRequestDTO(DEBIT_CARD, 1, new BigDecimal("1.4900"), new BigDecimal("0.00")),
            new FeeRuleRequestDTO(PIX, 1, new BigDecimal("0.9900"), new BigDecimal("0.00")),
            new FeeRuleRequestDTO(BOLETO, 1, new BigDecimal("0.0000"), new BigDecimal("3.49")));

    // Ten-slot cycle weighted toward cards and PIX, like a small retailer's mix.
    private static final PaymentMethod[] METHODS = {
            CREDIT_CARD, PIX, CREDIT_CARD, DEBIT_CARD, PIX, CREDIT_CARD, BOLETO, DEBIT_CARD, CREDIT_CARD, PIX
    };

    // Eleven values, coprime with the method cycle, so amounts and methods mix.
    private static final String[] AMOUNTS = {
            "149.90", "89.00", "1250.00", "45.50", "320.00", "59.99", "780.40", "19.90", "235.00", "99.00", "540.75"
    };

    // Days from sale to payout, capped at the last day of the window.
    private static final Map<PaymentMethod, Integer> SETTLEMENT_DELAY_DAYS = Map.of(
            CREDIT_CARD, 2, DEBIT_CARD, 1, PIX, 0, BOLETO, 1);

    private static final BigDecimal AMOUNT_GAP = new BigDecimal("10.00");
    private static final BigDecimal FEE_GAP = new BigDecimal("2.50");

    private enum Anomaly { MISSING_SETTLEMENT, INCORRECT_AMOUNT, FEE_DIVERGENCE, MISSING_BANK_CREDIT }

    // Transaction sequence numbers that break on purpose, two of each kind.
    private static final Map<Integer, Anomaly> ANOMALIES = Map.of(
            18, Anomaly.MISSING_SETTLEMENT, 131, Anomaly.MISSING_SETTLEMENT,
            44, Anomaly.INCORRECT_AMOUNT, 157, Anomaly.INCORRECT_AMOUNT,
            71, Anomaly.FEE_DIVERGENCE, 176, Anomaly.FEE_DIVERGENCE,
            97, Anomaly.MISSING_BANK_CREDIT, 189, Anomaly.MISSING_BANK_CREDIT);

    // Payouts with no sale behind them (ORPHAN_SETTLEMENT), numbered after the last transaction.
    private record Orphan(int sequence, int dayOffset, PaymentMethod method, BigDecimal amount) {
    }

    private static final List<Orphan> ORPHANS = List.of(
            new Orphan(TRANSACTIONS + 1, 25, PIX, new BigDecimal("410.00")),
            new Orphan(TRANSACTIONS + 2, 52, CREDIT_CARD, new BigDecimal("1890.00")));

    public static DemoScenario build(LocalDate today) {
        LocalDate from = today.minusDays(WINDOW_DAYS);
        LocalDate to = today.minusDays(1);

        StringBuilder transactions = header(TransactionCsvParser.HEADER);
        StringBuilder settlements = header(SettlementLayout.RECONPAY.header());
        StringBuilder bankLines = header(BankStatementCsvParser.EXPECTED_HEADER);

        for (int sequence = 1; sequence <= TRANSACTIONS; sequence++) {
            int index = sequence - 1;
            PaymentMethod method = METHODS[index % METHODS.length];
            BigDecimal amount = new BigDecimal(AMOUNTS[index % AMOUNTS.length]);
            LocalDate saleDate = from.plusDays((long) index * WINDOW_DAYS / TRANSACTIONS);
            String reference = reference(sequence);

            row(transactions, reference, amount.toPlainString(), method.name(), "1",
                    TransactionStatus.APPROVED.name(), saleDate.toString());

            Anomaly anomaly = ANOMALIES.get(sequence);
            if (anomaly == Anomaly.MISSING_SETTLEMENT) {
                continue;
            }

            BigDecimal settledAmount = anomaly == Anomaly.INCORRECT_AMOUNT ? amount.add(AMOUNT_GAP) : amount;
            BigDecimal netAmount = expectedNet(amount, method);
            if (anomaly == Anomaly.FEE_DIVERGENCE) {
                netAmount = netAmount.subtract(FEE_GAP);
            }
            LocalDate settlementDate = min(saleDate.plusDays(SETTLEMENT_DELAY_DAYS.get(method)), to);

            settlement(settlements, reference, settledAmount, netAmount, method, settlementDate);
            if (anomaly != Anomaly.MISSING_BANK_CREDIT) {
                bankLine(bankLines, sequence, reference, netAmount, settlementDate);
            }
        }

        for (Orphan orphan : ORPHANS) {
            String reference = reference(orphan.sequence());
            BigDecimal netAmount = expectedNet(orphan.amount(), orphan.method());
            LocalDate settlementDate = from.plusDays(orphan.dayOffset());
            settlement(settlements, reference, orphan.amount(), netAmount, orphan.method(), settlementDate);
            bankLine(bankLines, orphan.sequence(), reference, netAmount, settlementDate);
        }

        return new DemoScenario(
                from, to, FEE_RULES, transactions.toString(), settlements.toString(), bankLines.toString());
    }

    private static BigDecimal expectedNet(BigDecimal amount, PaymentMethod method) {
        FeeRuleRequestDTO rule = FEE_RULES.stream()
                .filter(candidate -> candidate.paymentMethod() == method)
                .findFirst()
                .orElseThrow();
        FeeRuleEntity entity = new FeeRuleEntity();
        entity.setFeePercentage(rule.feePercentage());
        entity.setFixedFee(rule.fixedFee());
        return ExpectedNetAmountCalculator.calculate(amount, entity);
    }

    private static void settlement(
            StringBuilder csv,
            String reference,
            BigDecimal amount,
            BigDecimal netAmount,
            PaymentMethod method,
            LocalDate settlementDate) {
        row(csv, reference, amount.toPlainString(), netAmount.toPlainString(), method.name(), "1",
                TransactionStatus.APPROVED.name(), settlementDate.toString());
    }

    private static void bankLine(
            StringBuilder csv, int sequence, String reference, BigDecimal amount, LocalDate movementDate) {
        row(csv, "BANK-%04d".formatted(sequence), reference, amount.toPlainString(), movementDate.toString());
    }

    private static String reference(int sequence) {
        return "DEMO-%04d".formatted(sequence);
    }

    private static LocalDate min(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? b : a;
    }

    private static StringBuilder header(String[] columns) {
        return new StringBuilder(String.join(",", columns)).append('\n');
    }

    private static void row(StringBuilder csv, String... columns) {
        csv.append(String.join(",", columns)).append('\n');
    }
}
