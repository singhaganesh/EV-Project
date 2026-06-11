package com.ganesh.EV_Project.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Money helpers. Monetary values are rounded to 2 decimal places (paise) before
 * persistence and stored in NUMERIC(10,2) columns so amounts are exact and sums
 * computed in SQL do not accumulate floating-point drift.
 */
public final class MoneyUtil {

    private MoneyUtil() {
    }

    /** Rounds a monetary amount to 2 decimal places using banker-safe HALF_UP. */
    public static double round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
