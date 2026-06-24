package br.com.hanrry.reconpay.feeRule.entity;

import br.com.hanrry.reconpay.feeRule.enums.PaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Table
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeeRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    private UUID merchantId;

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;



}
