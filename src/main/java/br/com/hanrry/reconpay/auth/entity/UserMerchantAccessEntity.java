package br.com.hanrry.reconpay.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_merchants")
@IdClass(UserMerchantAccessEntity.UserMerchantId.class)
public class UserMerchantAccessEntity {

    @Id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Id
    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt;

    public UserMerchantAccessEntity(UUID userId, UUID merchantId) {
        this.userId = userId;
        this.merchantId = merchantId;
    }

    @PrePersist
    protected void onCreate() {
        this.grantedAt = Instant.now();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserMerchantId implements Serializable {

        private UUID userId;
        private UUID merchantId;

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof UserMerchantId that)) {
                return false;
            }
            return Objects.equals(userId, that.userId) && Objects.equals(merchantId, that.merchantId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, merchantId);
        }
    }
}
