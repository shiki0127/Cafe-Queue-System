package com.cafequeue.order.infrastructure.persistence;

import com.cafequeue.order.domain.OrderStateHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStateHistoryRepository extends JpaRepository<OrderStateHistoryEntity, Long> {
}
