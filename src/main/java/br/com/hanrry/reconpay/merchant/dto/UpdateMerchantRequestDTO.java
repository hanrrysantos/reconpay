package br.com.hanrry.reconpay.merchant.dto;

import jakarta.validation.constraints.Size;

public record UpdateMerchantRequestDTO(

        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String name
) {
}
