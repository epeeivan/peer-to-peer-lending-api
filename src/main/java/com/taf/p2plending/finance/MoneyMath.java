package com.taf.p2plending.finance;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MoneyMath {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_EVEN);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);
    private static final BigDecimal ONE_PERCENT = new BigDecimal("0.01");
    private static final BigDecimal CENT = new BigDecimal("0.01");

    private MoneyMath() {
    }

    public static BigDecimal monthlyPayment(BigDecimal principal, BigDecimal annualRate, int termMonths) {
        BigDecimal i = annualRate.divide(TWELVE, MC);
        if (i.signum() == 0) {
            return Money.scale2(principal.divide(BigDecimal.valueOf(termMonths), MC));
        }
        BigDecimal pow = BigDecimal.ONE.add(i).pow(termMonths, MC);
        BigDecimal numerator = principal.multiply(i, MC).multiply(pow, MC);
        BigDecimal denominator = pow.subtract(BigDecimal.ONE, MC);
        return Money.scale2(numerator.divide(denominator, MC));
    }

    public static BigDecimal interestPortion(BigDecimal remainingPrincipal, BigDecimal annualRate) {
        BigDecimal i = annualRate.divide(TWELVE, MC);
        return Money.scale2(remainingPrincipal.multiply(i, MC));
    }

    public static BigDecimal platformFee(BigDecimal interestPortion) {
        return Money.scale2(interestPortion.multiply(ONE_PERCENT, MC));
    }

    public static List<BigDecimal> distribute(BigDecimal distributable, List<BigDecimal> amounts,
                                              BigDecimal totalPrincipal) {
        int n = amounts.size();
        List<BigDecimal> shares = new ArrayList<>(n);
        BigDecimal[] remainder = new BigDecimal[n];
        BigDecimal allocated = BigDecimal.ZERO;
        for (int k = 0; k < n; k++) {
            BigDecimal exact = distributable.multiply(amounts.get(k), MC).divide(totalPrincipal, MC);
            BigDecimal floored = exact.setScale(Money.SCALE, RoundingMode.DOWN);
            remainder[k] = exact.subtract(floored);
            shares.add(floored);
            allocated = allocated.add(floored);
        }
        int cents = Money.scale2(distributable).subtract(allocated)
                .movePointRight(Money.SCALE).setScale(0, RoundingMode.HALF_UP).intValueExact();
        List<Integer> order = new ArrayList<>(n);
        for (int k = 0; k < n; k++) {
            order.add(k);
        }
        order.sort(Comparator
                .comparing((Integer k) -> remainder[k]).reversed()
                .thenComparing(k -> amounts.get(k), Comparator.reverseOrder())
                .thenComparing(Comparator.naturalOrder()));
        for (int c = 0; c < cents; c++) {
            int idx = order.get(c % n);
            shares.set(idx, shares.get(idx).add(CENT));
        }
        List<BigDecimal> result = new ArrayList<>(n);
        for (BigDecimal share : shares) {
            result.add(Money.scale2(share));
        }
        return result;
    }
}
