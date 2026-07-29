package br.com.hanrry.reconpay.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MerchantRequestDTO(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String name,

        @NotBlank(message = "Documento é obrigatório")
        @Size(max = 30, message = "Documento deve ter no máximo 30 caracteres")
        String document
) {
}
