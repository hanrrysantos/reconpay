package br.com.hanrry.reconpay.openapi;

import io.swagger.v3.oas.models.tags.Tag;

import java.util.List;

public final class OpenApiTags {

    public static final String AUTHENTICATION = "Authentication";
    public static final String SESSION = "Session";
    public static final String USERS = "Users";
    public static final String MERCHANTS = "Merchants";
    public static final String TRANSACTIONS = "Transactions";
    public static final String FEE_RULES = "Fee Rules";
    public static final String EXTERNAL_SETTLEMENTS = "External Settlements";
    public static final String BANK_STATEMENTS = "Bank Statements";
    public static final String RECONCILIATIONS = "Reconciliations";

    private static final List<Tag> ORDERED = List.of(
            described(AUTHENTICATION, "Login, cadastro e verificação de e-mail, sem token."),
            described(SESSION, "Usuário autenticado e os estabelecimentos que ele pode acessar."),
            described(USERS, "Administração de usuários, ativação e concessão de acesso."),
            described(MERCHANTS, "Cadastro e manutenção dos estabelecimentos."),
            described(FEE_RULES, "Regras de cobrança e taxa do estabelecimento."),
            described(TRANSACTIONS, "Transações de venda registradas no estabelecimento."),
            described(EXTERNAL_SETTLEMENTS, "Liquidações recebidas e a importação do arquivo."),
            described(BANK_STATEMENTS, "Extrato bancário e a importação das linhas do arquivo."),
            described(RECONCILIATIONS, "Execução da conciliação e o tratamento das divergências.")
    );

    private OpenApiTags() {
    }

    public static List<Tag> ordered() {
        return ORDERED.stream()
                .map(tag -> new Tag().name(tag.getName()).description(tag.getDescription()))
                .toList();
    }

    private static Tag described(String name, String description) {
        return new Tag().name(name).description(description);
    }
}
