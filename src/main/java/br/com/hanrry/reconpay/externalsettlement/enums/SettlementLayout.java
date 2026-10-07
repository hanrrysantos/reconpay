package br.com.hanrry.reconpay.externalsettlement.enums;

public enum SettlementLayout {

    RECONPAY(
            "externalReference",
            "amount",
            "netAmount",
            "paymentMethod",
            "installments",
            "status",
            "settlementDate"),
    ACQUIRER(
            "nsu",
            "valor_bruto",
            "valor_liquido",
            "forma_pagamento",
            "parcelas",
            "situacao",
            "data_liquidacao");

    private final String[] header;

    SettlementLayout(String... header) {
        this.header = header.clone();
    }

    public String[] header() {
        return header.clone();
    }
}
