package com.cafequeue.common.contract;

import java.math.BigDecimal;

public record CouponLockRequest(String studentId, String couponCode, BigDecimal orderAmount, String orderId) {
}
