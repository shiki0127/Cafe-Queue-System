package com.cafequeue.order.infrastructure.persistence;

import com.cafequeue.order.domain.PaymentCallbackLogEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCallbackLogRepository extends JpaRepository<PaymentCallbackLogEntity, Long> {
    Optional<PaymentCallbackLogEntity> findByCallbackId(String callbackId);
}
