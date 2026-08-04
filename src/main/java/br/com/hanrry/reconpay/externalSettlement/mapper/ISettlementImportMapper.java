package br.com.hanrry.reconpay.externalSettlement.mapper;

import br.com.hanrry.reconpay.externalSettlement.dto.SettlementImportResponseDTO;
import br.com.hanrry.reconpay.externalSettlement.entity.SettlementImportEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ISettlementImportMapper {

    @Mapping(source = "merchant.id", target = "merchantId")
    SettlementImportResponseDTO toDTO(SettlementImportEntity entity);
}
