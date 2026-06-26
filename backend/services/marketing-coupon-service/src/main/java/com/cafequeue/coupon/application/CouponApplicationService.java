package com.cafequeue.coupon.application;

import com.cafequeue.common.contract.CouponLockRequest;
import com.cafequeue.common.contract.CouponLockResponse;
import com.cafequeue.common.core.BusinessException;
import com.cafequeue.common.core.IdGenerator;
import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CouponApplicationService {
    private static final String DEFAULT_TEMPLATE = "WELCOME_3";

    private final JdbcTemplate jdbcTemplate;

    public CouponApplicationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        seedTemplate();
    }

    public UserCouponView issue(String studentId, String templateCode) {
        String actualTemplateCode = templateCode == null || templateCode.isBlank() ? DEFAULT_TEMPLATE : templateCode;
        CouponTemplate template = findTemplate(actualTemplateCode);
        if (!template.enabled()) {
            throw new BusinessException("COUPON_TEMPLATE_DISABLED", "coupon template is disabled: " + actualTemplateCode);
        }
        if (template.issuedQuantity() >= template.totalQuantity()) {
            throw new BusinessException("COUPON_TEMPLATE_EMPTY", "coupon template quantity is empty: " + actualTemplateCode);
        }
        String couponCode = IdGenerator.prefixed("cpn");
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT INTO user_coupon(coupon_code, template_code, student_id, status, created_at)
                VALUES (?, ?, ?, ?, ?)
                """, couponCode, actualTemplateCode, studentId, "ISSUED", Timestamp.from(now));
        jdbcTemplate.update("""
                UPDATE coupon_template
                SET issued_quantity = issued_quantity + 1, updated_at = ?
                WHERE template_code = ?
                """, Timestamp.from(now), actualTemplateCode);
        return new UserCouponView(couponCode, studentId, actualTemplateCode, "ISSUED", template.discountAmount(), template.thresholdAmount(), null, now);
    }

    public CouponLockResponse lock(CouponLockRequest request) {
        if (request.couponCode() == null || request.couponCode().isBlank()) {
            return new CouponLockResponse(null, BigDecimal.ZERO, true);
        }
        List<UserCouponView> coupons = queryCoupon(request.couponCode());
        if (coupons.isEmpty()) {
            return new CouponLockResponse(null, BigDecimal.ZERO, false);
        }
        UserCouponView coupon = coupons.getFirst();
        if (!coupon.studentId().equals(request.studentId()) || request.orderAmount().compareTo(coupon.thresholdAmount()) < 0) {
            return new CouponLockResponse(null, BigDecimal.ZERO, false);
        }
        if ("LOCKED".equals(coupon.status())) {
            return new CouponLockResponse(coupon.lockedOrderId(), coupon.discountAmount(), request.orderId().equals(coupon.lockedOrderId()));
        }
        if (!"ISSUED".equals(coupon.status())) {
            return new CouponLockResponse(null, BigDecimal.ZERO, false);
        }
        int updated = jdbcTemplate.update("""
                UPDATE user_coupon
                SET status = ?, locked_order_id = ?
                WHERE coupon_code = ? AND status = ?
                """, "LOCKED", request.orderId(), request.couponCode(), "ISSUED");
        return new CouponLockResponse(request.orderId(), coupon.discountAmount(), updated == 1);
    }

    public UserCouponView redeem(String couponCode) {
        List<UserCouponView> coupons = queryCoupon(couponCode);
        if (coupons.isEmpty()) {
            throw new BusinessException("COUPON_NOT_FOUND", "coupon not found: " + couponCode);
        }
        UserCouponView coupon = coupons.getFirst();
        if ("REDEEMED".equals(coupon.status())) {
            return coupon;
        }
        if (!"LOCKED".equals(coupon.status())) {
            throw new BusinessException("COUPON_NOT_LOCKED", "coupon is not locked: " + couponCode);
        }
        Instant now = Instant.now();
        jdbcTemplate.update("""
                UPDATE user_coupon
                SET status = ?, used_at = ?
                WHERE coupon_code = ? AND status = ?
                """, "REDEEMED", Timestamp.from(now), couponCode, "LOCKED");
        try {
            jdbcTemplate.update("""
                    INSERT INTO coupon_redeem_log(coupon_code, order_id, idempotency_key, created_at)
                    VALUES (?, ?, ?, ?)
                    """, couponCode, coupon.lockedOrderId(), couponCode + ":" + coupon.lockedOrderId(), Timestamp.from(now));
        } catch (DuplicateKeyException ignored) {
            // Redeem callbacks are idempotent for the same coupon/order pair.
        }
        return new UserCouponView(coupon.couponCode(), coupon.studentId(), coupon.templateCode(), "REDEEMED", coupon.discountAmount(), coupon.thresholdAmount(), coupon.lockedOrderId(), coupon.createdAt());
    }

    public UserCouponView release(String couponCode) {
        List<UserCouponView> coupons = queryCoupon(couponCode);
        if (coupons.isEmpty()) {
            throw new BusinessException("COUPON_NOT_FOUND", "coupon not found: " + couponCode);
        }
        UserCouponView coupon = coupons.getFirst();
        if (!"LOCKED".equals(coupon.status())) {
            return coupon;
        }
        jdbcTemplate.update("""
                UPDATE user_coupon
                SET status = ?, locked_order_id = NULL
                WHERE coupon_code = ? AND status = ?
                """, "ISSUED", couponCode, "LOCKED");
        return new UserCouponView(coupon.couponCode(), coupon.studentId(), coupon.templateCode(), "ISSUED", coupon.discountAmount(), coupon.thresholdAmount(), null, coupon.createdAt());
    }

    private List<UserCouponView> queryCoupon(String couponCode) {
        return jdbcTemplate.query("""
                SELECT uc.coupon_code, uc.student_id, uc.template_code, uc.status, uc.locked_order_id,
                       uc.created_at, ct.discount_amount, ct.threshold_amount
                FROM user_coupon uc
                JOIN coupon_template ct ON uc.template_code = ct.template_code
                WHERE uc.coupon_code = ?
                """, (rs, rowNum) -> new UserCouponView(
                rs.getString("coupon_code"),
                rs.getString("student_id"),
                rs.getString("template_code"),
                rs.getString("status"),
                rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("threshold_amount"),
                rs.getString("locked_order_id"),
                rs.getTimestamp("created_at").toInstant()
        ), couponCode);
    }

    private CouponTemplate findTemplate(String templateCode) {
        List<CouponTemplate> templates = jdbcTemplate.query("""
                SELECT template_code, discount_amount, threshold_amount, total_quantity, issued_quantity, enabled
                FROM coupon_template
                WHERE template_code = ?
                """, (rs, rowNum) -> new CouponTemplate(
                rs.getString("template_code"),
                rs.getBigDecimal("discount_amount"),
                rs.getBigDecimal("threshold_amount"),
                rs.getInt("total_quantity"),
                rs.getInt("issued_quantity"),
                rs.getBoolean("enabled")
        ), templateCode);
        if (templates.isEmpty()) {
            throw new BusinessException("COUPON_TEMPLATE_NOT_FOUND", "coupon template not found: " + templateCode);
        }
        return templates.getFirst();
    }

    private void seedTemplate() {
        Instant now = Instant.now();
        jdbcTemplate.update("""
                INSERT IGNORE INTO coupon_template(template_code, template_name, discount_amount, threshold_amount, total_quantity, issued_quantity, enabled, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, DEFAULT_TEMPLATE, "Welcome discount", BigDecimal.valueOf(3), BigDecimal.ZERO, 10000, 0, true, Timestamp.from(now), Timestamp.from(now));
    }

    public record UserCouponView(
            String couponCode,
            String studentId,
            String templateCode,
            String status,
            BigDecimal discountAmount,
            BigDecimal thresholdAmount,
            String lockedOrderId,
            Instant createdAt
    ) {
    }

    private record CouponTemplate(
            String templateCode,
            BigDecimal discountAmount,
            BigDecimal thresholdAmount,
            int totalQuantity,
            int issuedQuantity,
            boolean enabled
    ) {
    }
}
