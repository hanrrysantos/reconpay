package br.com.hanrry.reconpay.merchant.service;

import br.com.hanrry.reconpay.auth.enums.UserRole;
import br.com.hanrry.reconpay.auth.repository.IUserMerchantAccessRepository;
import br.com.hanrry.reconpay.auth.service.UserMerchantAccessService;
import br.com.hanrry.reconpay.exception.MerchantAlreadyExistsException;
import br.com.hanrry.reconpay.exception.MerchantNotFoundException;
import br.com.hanrry.reconpay.merchant.dto.MerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.dto.MerchantResponseDTO;
import br.com.hanrry.reconpay.merchant.dto.UpdateMerchantRequestDTO;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.mapper.IMerchantMapper;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.observability.AuditLogger;
import br.com.hanrry.reconpay.security.AuthenticatedUserAccessor;
import br.com.hanrry.reconpay.security.CustomUserDetails;
import br.com.hanrry.reconpay.security.MerchantAccessGuard;
import br.com.hanrry.reconpay.auth.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MerchantServiceTest {

    @Mock
    private IMerchantMapper merchantMapper;

    @Mock
    private IMerchantRepository merchantRepository;

    @Mock
    private IUserMerchantAccessRepository userMerchantAccessRepository;

    @Mock
    private UserMerchantAccessService userMerchantAccessService;

    @Mock
    private MerchantAccessGuard merchantAccessGuard;

    @Mock
    private AuthenticatedUserAccessor authenticatedUserAccessor;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private MerchantService merchantService;

    private UUID adminUserId;

    @BeforeEach
    void setUpPrincipal() {
        adminUserId = UUID.randomUUID();
        UserEntity admin = new UserEntity();
        admin.setId(adminUserId);
        admin.setEmail("admin@test.local");
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        when(authenticatedUserAccessor.requirePrincipal()).thenReturn(new CustomUserDetails(admin));
    }

    @Test
    void createShouldPersistMerchantWhenDocumentIsUnique() {
        MerchantRequestDTO request = new MerchantRequestDTO("Loja Exemplo", "12345678000199");
        MerchantEntity mappedEntity = buildMerchant(null, "Loja Exemplo", "12345678000199");
        MerchantEntity savedEntity = buildMerchant(UUID.randomUUID(), "Loja Exemplo", "12345678000199");
        MerchantResponseDTO expectedResponse = toResponseDTO(savedEntity);

        when(merchantRepository.existsByDocument("12345678000199")).thenReturn(false);
        when(merchantMapper.toEntity(request)).thenReturn(mappedEntity);
        when(merchantRepository.save(mappedEntity)).thenReturn(savedEntity);
        when(merchantMapper.toDTO(savedEntity)).thenReturn(expectedResponse);

        MerchantResponseDTO response = merchantService.create(request);

        verify(merchantRepository).save(mappedEntity);
        verify(userMerchantAccessService).grantIfAbsent(eq(adminUserId), eq(savedEntity.getId()));
        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void createShouldPropagateWhenAutoGrantFails() {
        MerchantRequestDTO request = new MerchantRequestDTO("Loja Exemplo", "99999999000199");
        MerchantEntity mappedEntity = buildMerchant(null, "Loja Exemplo", "99999999000199");
        MerchantEntity savedEntity = buildMerchant(UUID.randomUUID(), "Loja Exemplo", "99999999000199");

        when(merchantRepository.existsByDocument("99999999000199")).thenReturn(false);
        when(merchantMapper.toEntity(request)).thenReturn(mappedEntity);
        when(merchantRepository.save(mappedEntity)).thenReturn(savedEntity);
        doThrow(new IllegalStateException("grant failed"))
                .when(userMerchantAccessService).grantIfAbsent(eq(adminUserId), eq(savedEntity.getId()));

        assertThatThrownBy(() -> merchantService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("grant failed");
    }

    @Test
    void createShouldThrowWhenDocumentAlreadyExists() {
        MerchantRequestDTO request = new MerchantRequestDTO("Loja B", "12345678000199");

        when(merchantRepository.existsByDocument("12345678000199")).thenReturn(true);

        assertThatThrownBy(() -> merchantService.create(request))
                .isInstanceOf(MerchantAlreadyExistsException.class)
                .hasMessage("Comerciante já cadastrado com documento: 12345678000199");

        verify(merchantMapper, never()).toEntity(any());
        verify(merchantRepository, never()).save(any());
    }

    @Test
    void findByIdShouldReturnActiveMerchant() {
        UUID merchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId, "Loja Exemplo", "12345678000199");
        MerchantResponseDTO expectedResponse = toResponseDTO(merchant);

        doNothing().when(merchantAccessGuard).requireAccess(merchantId);
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(merchantMapper.toDTO(merchant)).thenReturn(expectedResponse);

        MerchantResponseDTO response = merchantService.findById(merchantId);

        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void findByIdShouldThrowWhenMerchantNotFound() {
        UUID merchantId = UUID.randomUUID();

        doNothing().when(merchantAccessGuard).requireAccess(merchantId);
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchantService.findById(merchantId))
                .isInstanceOf(MerchantNotFoundException.class)
                .hasMessage("Comerciante não encontrado com id: " + merchantId);
    }

    @Test
    void findAllActiveShouldReturnMappedPage() {
        MerchantEntity merchant = buildMerchant(UUID.randomUUID(), "Loja Exemplo", "12345678000199");
        MerchantResponseDTO expectedResponse = toResponseDTO(merchant);
        Page<MerchantEntity> page = new PageImpl<>(List.of(merchant));

        when(merchantRepository.findAllByActiveTrue(PageRequest.of(0, 20))).thenReturn(page);
        when(merchantMapper.toDTO(merchant)).thenReturn(expectedResponse);

        Page<MerchantResponseDTO> result = merchantService.findAllActive(PageRequest.of(0, 20));

        assertThat(result.getContent()).containsExactly(expectedResponse);
    }

    @Test
    void updateShouldPersistNewName() {
        UUID merchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId, "Loja Exemplo", "12345678000199");
        MerchantEntity savedMerchant = buildMerchant(merchantId, "Loja Atualizada", "12345678000199");
        MerchantResponseDTO expectedResponse = toResponseDTO(savedMerchant);

        doNothing().when(merchantAccessGuard).requireAccess(merchantId);
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));
        when(merchantRepository.save(merchant)).thenReturn(savedMerchant);
        when(merchantMapper.toDTO(savedMerchant)).thenReturn(expectedResponse);

        MerchantResponseDTO response = merchantService.update(
                merchantId,
                new UpdateMerchantRequestDTO("Loja Atualizada"));

        assertThat(merchant.getName()).isEqualTo("Loja Atualizada");
        assertThat(response).isEqualTo(expectedResponse);
    }

    @Test
    void updateShouldThrowWhenMerchantNotFound() {
        UUID merchantId = UUID.randomUUID();

        doNothing().when(merchantAccessGuard).requireAccess(merchantId);
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchantService.update(
                merchantId,
                new UpdateMerchantRequestDTO("Loja Atualizada")))
                .isInstanceOf(MerchantNotFoundException.class);

        verify(merchantRepository, never()).save(any());
    }

    @Test
    void deleteByIdShouldSoftDeleteMerchant() {
        UUID merchantId = UUID.randomUUID();
        MerchantEntity merchant = buildMerchant(merchantId, "Loja Exemplo", "12345678000199");

        doNothing().when(merchantAccessGuard).requireAccess(merchantId);
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(merchant));

        merchantService.deleteById(merchantId);

        ArgumentCaptor<MerchantEntity> entityCaptor = ArgumentCaptor.forClass(MerchantEntity.class);
        verify(merchantRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().isActive()).isFalse();
    }

    @Test
    void deleteByIdShouldThrowWhenMerchantNotFound() {
        UUID merchantId = UUID.randomUUID();

        doNothing().when(merchantAccessGuard).requireAccess(merchantId);
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> merchantService.deleteById(merchantId))
                .isInstanceOf(MerchantNotFoundException.class);

        verify(merchantRepository, never()).save(any());
    }

    private MerchantEntity buildMerchant(UUID id, String name, String document) {
        MerchantEntity merchant = new MerchantEntity();
        merchant.setId(id);
        merchant.setName(name);
        merchant.setDocument(document);
        merchant.setActive(true);
        merchant.setCreatedAt(Instant.parse("2026-08-01T12:00:00Z"));
        merchant.setUpdatedAt(Instant.parse("2026-08-01T12:00:00Z"));
        return merchant;
    }

    private MerchantResponseDTO toResponseDTO(MerchantEntity merchant) {
        return new MerchantResponseDTO(
                merchant.getId(),
                merchant.getName(),
                merchant.getDocument(),
                merchant.isActive(),
                merchant.getCreatedAt()
        );
    }
}
