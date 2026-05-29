package br.com.hanrry.reconpay.merchant.service;

import br.com.hanrry.reconpay.merchant.dto.MerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.dto.MerchantResponseDTO;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.exception.MerchantAlreadyExistsException;
import br.com.hanrry.reconpay.merchant.mapper.IMerchantMapper;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final IMerchantMapper mapper;
    private final IMerchantRepository repository;

    @Transactional
    public MerchantResponseDTO createMerchant(MerchantRequestDTO request){
        if(!repository.existsByDocument(request.document())){
            throw new MerchantAlreadyExistsException("Merchant al ready exists with this document: " + request.document());
        }

        MerchantEntity entity = mapper.toEntity(request);

        MerchantEntity savedMerchant = repository.save(entity);

        return mapper.toDTO(savedMerchant);
    }
}
