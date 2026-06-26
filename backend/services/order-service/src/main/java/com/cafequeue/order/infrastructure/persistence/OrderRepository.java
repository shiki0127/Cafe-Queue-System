package com.cafequeue.order.infrastructure.persistence;

import com.cafequeue.order.domain.OrderEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    Optional<OrderEntity> findByOrderId(String orderId);

    boolean existsByOrderId(String orderId);
}
