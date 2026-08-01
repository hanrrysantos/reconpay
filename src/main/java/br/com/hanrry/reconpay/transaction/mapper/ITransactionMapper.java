package br.com.hanrry.reconpay.transaction.mapper;

import br.com.hanrry.reconpay.transaction.dto.TransactionResponseDTO;
import br.com.hanrry.reconpay.transaction.entity.InternalTransactionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ITransactionMapper {

    @Mapping(source = "merchant.id", target = "merchantId")
    TransactionResponseDTO toDTO(InternalTransactionEntity entity);
}
