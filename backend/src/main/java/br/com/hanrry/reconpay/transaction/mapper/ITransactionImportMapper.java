package br.com.hanrry.reconpay.transaction.mapper;

import br.com.hanrry.reconpay.transaction.dto.TransactionImportResponseDTO;
import br.com.hanrry.reconpay.transaction.entity.TransactionImportEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ITransactionImportMapper {

    @Mapping(source = "merchant.id", target = "merchantId")
    TransactionImportResponseDTO toDTO(TransactionImportEntity entity);
}
