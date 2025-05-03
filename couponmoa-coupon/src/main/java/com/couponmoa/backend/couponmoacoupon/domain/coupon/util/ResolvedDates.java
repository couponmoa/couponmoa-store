package com.couponmoa.backend.couponmoacoupon.domain.coupon.util;

import java.time.LocalDateTime;

public record ResolvedDates(
     LocalDateTime startDate,
     LocalDateTime endDate,
     LocalDateTime expiryDate
) {}