package br.com.hanrry.reconpay.auth.service;

import br.com.hanrry.reconpay.auth.entity.UserMerchantAccessEntity;
import br.com.hanrry.reconpay.auth.repository.IUserMerchantAccessRepository;
import br.com.hanrry.reconpay.auth.repository.IUserRepository;
import br.com.hanrry.reconpay.merchant.entity.MerchantEntity;
import br.com.hanrry.reconpay.merchant.repository.IMerchantRepository;
import br.com.hanrry.reconpay.observability.AuditLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserMerchantAccessServiceTest {

    @Mock
    private IUserMerchantAccessRepository accessRepository;

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IMerchantRepository merchantRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private UserMerchantAccessService userMerchantAccessService;

    @Test
    void grantIfAbsentShouldSkipWhenGrantAlreadyExists() {
        UUID userId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(new MerchantEntity()));
        when(accessRepository.existsByUserIdAndMerchantId(userId, merchantId)).thenReturn(true);

        userMerchantAccessService.grantIfAbsent(userId, merchantId);

        verify(accessRepository, never()).save(any());
    }

    @Test
    void grantIfAbsentShouldPersistNewGrant() {
        UUID userId = UUID.randomUUID();
        UUID merchantId = UUID.randomUUID();

        when(userRepository.existsById(userId)).thenReturn(true);
        when(merchantRepository.findByIdAndActiveTrue(merchantId)).thenReturn(Optional.of(new MerchantEntity()));
        when(accessRepository.existsByUserIdAndMerchantId(userId, merchantId)).thenReturn(false);

        userMerchantAccessService.grantIfAbsent(userId, merchantId);

        verify(accessRepository).save(any(UserMerchantAccessEntity.class));
    }
}
