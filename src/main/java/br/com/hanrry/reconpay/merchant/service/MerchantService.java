package br.com.hanrry.reconpay.merchant.service;

import br.com.hanrry.reconpay.merchant.dto.MerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.dto.MerchantResponseDTO;
import br.com.hanrry.reconpay.merchant.dto.UpdateMerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.exception.MerchantAlreadyExistsException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
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

    public List<MerchantResponseDTO> findAllMerchantsByActiveTrue() {
        List<MerchantEntity> merchants = merchantRepository.findAllByActiveTrue();

        return merchantMapper.toDTOList(merchants);
    }

    public MerchantResponseDTO findMerchantByIdAndActiveTrue(UUID id){
        MerchantEntity entity = merchantRepository.findByIdAndActiveTrue(id).orElseThrow(
                () -> new MerchantNotFoundException("Merchant not found with this id: " + id)
        );

        return merchantMapper.toDTO(entity);
    }

    public MerchantResponseDTO updateMerchantById(UUID id, UpdateMerchantRequestDTO request) {
        MerchantEntity merchant = merchantRepository.findByIdAndActiveTrue(id).orElseThrow(
                () -> new MerchantNotFoundException("Merchant not found with this id: " + id)
        );

        if(request.name() != null && !request.name().isBlank()) {
            merchant.setName(request.name());
        }

        MerchantEntity savedMerchant = merchantRepository.save(merchant);

        return merchantMapper.toDTO(savedMerchant);
    }

    @Transactional
    public void deleteMerchantById(UUID id){
        MerchantEntity merchant = merchantRepository.findByIdAndActiveTrue(id).orElseThrow(
                () -> new MerchantNotFoundException("Merchant not found with this id: " + id)
        );

        merchant.setActive(false);
        merchantRepository.save(merchant);
    }

}
