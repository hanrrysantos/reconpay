package br.com.hanrry.reconpay.merchant.dto;

import br.com.hanrry.reconpay.merchant.validation.ValidCnpj;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MerchantRequestDTO(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String name,

        @NotBlank(message = "Documento é obrigatório")
        @ValidCnpj
        String document
) {
}
