package br.com.hanrry.reconpay.externalSettlement.mapper;

import br.com.hanrry.reconpay.externalSettlement.dto.ExternalSettlementResponseDTO;
import br.com.hanrry.reconpay.externalSettlement.entity.ExternalSettlementEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IExternalSettlementMapper {

    @Mapping(source = "merchant.id", target = "merchantId")
    @Mapping(source = "importBatch.id", target = "importId")
    ExternalSettlementResponseDTO toDTO(ExternalSettlementEntity entity);
}
