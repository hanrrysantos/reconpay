package br.com.hanrry.reconpay.merchant.service;

import br.com.hanrry.reconpay.exception.MerchantAlreadyExistsException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.merchant.dto.MerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.dto.MerchantResponseDTO;
import br.com.hanrry.reconpay.merchant.dto.UpdateMerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.mapper.IMerchantMapper;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final IMerchantMapper merchantMapper;
    private final IMerchantRepository merchantRepository;

    @Transactional
    public MerchantResponseDTO create(MerchantRequestDTO request) {
        if (merchantRepository.existsByDocument(request.document())) {
            throw new MerchantAlreadyExistsException(
                    "Comerciante já cadastrado com documento: " + request.document());
        }

        MerchantEntity entity = merchantMapper.toEntity(request);
        MerchantEntity savedMerchant = merchantRepository.save(entity);
        return merchantMapper.toDTO(savedMerchant);
    }

    public Page<MerchantResponseDTO> findAllActive(Pageable pageable) {
        return merchantRepository.findAllByActiveTrue(pageable)
                .map(merchantMapper::toDTO);
    }

    public MerchantResponseDTO findById(UUID id) {
        MerchantEntity entity = merchantRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new MerchantNotFoundException("Comerciante não encontrado com id: " + id));
        return merchantMapper.toDTO(entity);
    }

    @Transactional
    public MerchantResponseDTO update(UUID id, UpdateMerchantRequestDTO request) {
        MerchantEntity merchant = merchantRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new MerchantNotFoundException("Comerciante não encontrado com id: " + id));

        if (request.name() != null && !request.name().isBlank()) {
            merchant.setName(request.name());
        }

        MerchantEntity savedMerchant = merchantRepository.save(merchant);
        return merchantMapper.toDTO(savedMerchant);
    }

    @Transactional
    public void deleteById(UUID id) {
        MerchantEntity merchant = merchantRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new MerchantNotFoundException("Comerciante não encontrado com id: " + id));
        merchant.setActive(false);
        merchantRepository.save(merchant);
    }
}
