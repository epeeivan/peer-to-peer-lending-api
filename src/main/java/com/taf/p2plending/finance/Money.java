package com.taf.p2plending.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {

    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    private Money() {
    }

    public static BigDecimal scale2(BigDecimal value) {
        return value.setScale(SCALE, ROUNDING);
    }
}
