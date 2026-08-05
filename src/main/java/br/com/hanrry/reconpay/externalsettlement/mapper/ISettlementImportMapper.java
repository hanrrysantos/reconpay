package br.com.hanrry.reconpay.externalsettlement.mapper;

import br.com.hanrry.reconpay.externalsettlement.dto.SettlementImportResponseDTO;
import br.com.hanrry.reconpay.externalsettlement.entity.SettlementImportEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ISettlementImportMapper {

    @Mapping(source = "merchant.id", target = "merchantId")
    SettlementImportResponseDTO toDTO(SettlementImportEntity entity);
}
