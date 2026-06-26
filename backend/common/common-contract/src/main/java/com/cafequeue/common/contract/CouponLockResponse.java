package com.cafequeue.common.contract;

import java.math.BigDecimal;

public record CouponLockResponse(String couponLockId, BigDecimal discountAmount, boolean usable) {
}
