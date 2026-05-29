package br.com.hanrry.reconpay.merchant.mapper;

import br.com.hanrry.reconpay.merchant.dto.MerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.dto.MerchantResponseDTO;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IMerchantMapper {

    MerchantEntity toEntity(MerchantRequestDTO request);

    MerchantResponseDTO toDTO(MerchantEntity entity);

    Page<MerchantResponseDTO> toDTOList(Page<MerchantEntity> entities);


}
