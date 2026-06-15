package br.com.hanrry.reconpay.merchant.service;

import br.com.hanrry.reconpay.merchant.dto.MerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.dto.MerchantResponseDTO;
import br.com.hanrry.reconpay.merchant.dto.UpdateMerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.exception.MerchantAlreadyExistsException;
import br.com.hanrry.reconpay.merchant.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.merchant.mapper.IMerchantMapper;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final IMerchantMapper merchantMapper;
    private final IMerchantRepository merchantRepository;

    @Transactional
    public MerchantResponseDTO createMerchant(MerchantRequestDTO request){
        if(merchantRepository.existsByDocument(request.document())){
            throw new MerchantAlreadyExistsException("Merchant al ready exists with this document: " + request.document());
        }

        MerchantEntity entity = merchantMapper.toEntity(request);

        MerchantEntity savedMerchant = merchantRepository.save(entity);

        return merchantMapper.toDTO(savedMerchant);
    }

    public List<MerchantResponseDTO> findAllMerchants() {
        List<MerchantEntity> merchants = merchantRepository.findAll();

        return merchantMapper.toDTOList(merchants);
    }

    public MerchantResponseDTO findMerchantById(UUID id){
        MerchantEntity entity = merchantRepository.findById(id).orElseThrow(
                () -> new MerchantNotFoundException("Merchant not found with this id: " + id)
        );

        return merchantMapper.toDTO(entity);
    }

    public MerchantResponseDTO updateMerchantById(UUID id, UpdateMerchantRequestDTO request) {
        MerchantEntity merchant = merchantRepository.findById(id).orElseThrow(
                () -> new MerchantNotFoundException("Merchant not found with this id: " + id)
        );

        if(request.name() != null && !request.name().isBlank()) {
            merchant.setName(request.name());
        }

        MerchantEntity savedMerchant = merchantRepository.save(merchant);

        return merchantMapper.toDTO(merchant);
    }


    public void deleteMerchantById(UUID id){
        findMerchantById(id);
        merchantRepository.deleteById(id);
    }

}
