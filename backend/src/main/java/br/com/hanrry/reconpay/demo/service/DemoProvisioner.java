package br.com.hanrry.reconpay.demo.service;

import br.com.hanrry.reconpay.auth.entity.UserEntity;
import br.com.hanrry.reconpay.auth.enums.UserRole;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.auth.service.UserMerchantAccessService;
import br.com.hanrry.reconpay.bankstatement.service.BankStatementService;
import br.com.hanrry.reconpay.demo.config.DemoProperties;
import br.com.hanrry.reconpay.externalsettlement.enums.SettlementLayout;
import br.com.hanrry.reconpay.externalsettlement.service.ExternalSettlementService;
import br.com.hanrry.reconpay.feerule.dto.FeeRuleRequestDTO;
import br.com.hanrry.reconpay.feerule.service.FeeRuleService;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.reconciliation.dto.RunReconciliationRequestDTO;
import br.com.hanrry.reconpay.reconciliation.service.ReconciliationService;
import br.com.hanrry.reconpay.shared.CnpjRules;
import br.com.hanrry.reconpay.transaction.service.TransactionImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/*
 * Writes a demo account and its scenario in one transaction, through the same services a
 * visitor would use. The creation is anonymous, so the merchant is saved and granted here
 * rather than through MerchantService, which needs an authenticated creator.
 */
@Service
@RequiredArgsConstructor
public class DemoProvisioner {

    private static final String EMAIL_DOMAIN = "@demo.reconpay.invalid";
    private static final String USER_NAME = "Visitante";
    private static final String MERCHANT_NAME = "Loja Demonstração";

    private final SecureRandom random = new SecureRandom();

    private final IUserRepository userRepository;
    private final IMerchantRepository merchantRepository;
    private final UserMerchantAccessService userMerchantAccessService;
    private final FeeRuleService feeRuleService;
    private final TransactionImportService transactionImportService;
    private final ExternalSettlementService externalSettlementService;
    private final BankStatementService bankStatementService;
    private final ReconciliationService reconciliationService;
    private final PasswordEncoder passwordEncoder;
    private final DemoProperties properties;

    @Transactional
    public DemoAccount provision(Instant now) {
        UserEntity user = userRepository.save(newUser(now));
        MerchantEntity merchant = merchantRepository.save(newMerchant());
        userMerchantAccessService.grantIfAbsent(user.getId(), merchant.getId());

        UUID merchantId = merchant.getId();
        DemoScenario scenario = DemoScenario.build(LocalDate.ofInstant(now, ZoneOffset.UTC));
        for (FeeRuleRequestDTO rule : scenario.feeRules()) {
            feeRuleService.create(merchantId, rule);
        }
        transactionImportService.importCsv(merchantId,
                new InMemoryCsvFile("demo-transacoes.csv", scenario.transactionsCsv()));
        externalSettlementService.importCsv(merchantId,
                new InMemoryCsvFile("demo-liquidacoes.csv", scenario.settlementsCsv()), SettlementLayout.RECONPAY);
        bankStatementService.importCsv(merchantId,
                new InMemoryCsvFile("demo-extrato.csv", scenario.bankStatementCsv()));

        UUID runId = reconciliationService.run(merchantId,
                new RunReconciliationRequestDTO(scenario.fromDate(), scenario.toDate())).id();
        return new DemoAccount(user.getId(), user.getEmail(), runId);
    }

    private UserEntity newUser(Instant now) {
        UserEntity user = new UserEntity();
        user.setName(USER_NAME);
        user.setEmail("demo-" + HexFormat.of().formatHex(randomBytes(6)) + EMAIL_DOMAIN);
        // Nobody signs in with it; the account is reached only through the issued token.
        user.setPassword(passwordEncoder.encode(Base64.getEncoder().encodeToString(randomBytes(32))));
        user.setRole(UserRole.OPERATOR);
        user.setActive(true);
        user.setDemoExpiresAt(now.plus(properties.ttl()));
        return user;
    }

    private MerchantEntity newMerchant() {
        String document;
        do {
            document = CnpjRules.generate(random);
        } while (merchantRepository.existsByDocumentDigits(document));

        MerchantEntity merchant = new MerchantEntity();
        merchant.setName(MERCHANT_NAME);
        merchant.setDocument(document);
        return merchant;
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return bytes;
    }
}
