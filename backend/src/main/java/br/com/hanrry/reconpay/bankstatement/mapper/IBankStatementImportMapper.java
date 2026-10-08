package br.com.hanrry.reconpay.bankstatement.mapper;

import br.com.hanrry.reconpay.bankstatement.dto.BankStatementImportResponseDTO;
import br.com.hanrry.reconpay.bankstatement.entity.BankStatementImportEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IBankStatementImportMapper {

    @Mapping(source = "merchant.id", target = "merchantId")
    BankStatementImportResponseDTO toDTO(BankStatementImportEntity entity);
}
