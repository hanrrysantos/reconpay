package br.com.hanrry.reconpay.merchant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMerchantRequestDTO (

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must be at most 150 characters")
        String name,

        @NotBlank(message = "Document is required")
        @Size(max = 30, message = "Document must be at most 30 characters")
        String document
){
}
