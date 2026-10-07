package br.com.hanrry.reconpay.bankstatement.mapper;

import br.com.hanrry.reconpay.bankstatement.dto.BankStatementLineResponseDTO;
import br.com.hanrry.reconpay.bankstatement.entity.BankStatementLineEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IBankStatementLineMapper {

    @Mapping(source = "importBatch.id", target = "importId")
    BankStatementLineResponseDTO toDTO(BankStatementLineEntity entity);
}
