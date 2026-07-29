package br.com.hanrry.reconpay.feeRule.mapper;

import br.com.hanrry.reconpay.feeRule.dto.FeeRuleResponseDTO;
import br.com.hanrry.reconpay.feeRule.entity.FeeRuleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface IFeeRuleMapper {

    @Mapping(source = "merchant.id", target = "merchantId")
    @Mapping(source = "merchant.name", target = "merchantName")
    @Mapping(source = "paymentMethod", target = "paymentMethod")
    FeeRuleResponseDTO toDTO(FeeRuleEntity entity);
}
