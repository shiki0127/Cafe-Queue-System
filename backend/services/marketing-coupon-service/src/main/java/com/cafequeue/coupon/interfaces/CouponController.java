package com.cafequeue.coupon.interfaces;

import com.cafequeue.common.contract.CouponLockRequest;
import com.cafequeue.common.contract.CouponLockResponse;
import com.cafequeue.common.core.ApiResponse;
import com.cafequeue.coupon.application.CouponApplicationService;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
public class CouponController {
    private final CouponApplicationService couponService;

    public CouponController(CouponApplicationService couponService) {
        this.couponService = couponService;
    }

    @PostMapping("/issue")
    public ApiResponse<CouponApplicationService.UserCouponView> issue(@RequestBody IssueCouponRequest request) {
        return ApiResponse.ok(couponService.issue(request.studentId(), request.templateCode()));
    }

    @PostMapping("/lock")
    public ApiResponse<CouponLockResponse> lock(@RequestBody CouponLockRequest request) {
        return ApiResponse.ok(couponService.lock(request));
    }

    @PostMapping("/{couponCode}/redeem")
    public ApiResponse<CouponApplicationService.UserCouponView> redeem(@PathVariable String couponCode) {
        return ApiResponse.ok(couponService.redeem(couponCode));
    }

    @PostMapping("/{couponCode}/release")
    public ApiResponse<CouponApplicationService.UserCouponView> release(@PathVariable String couponCode) {
        return ApiResponse.ok(couponService.release(couponCode));
    }

    public record IssueCouponRequest(String studentId, String templateCode, BigDecimal thresholdAmount) {
    }
}
