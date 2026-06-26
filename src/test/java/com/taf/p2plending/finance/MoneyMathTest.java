package com.taf.p2plending.finance;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MoneyMathTest {

    @Test
    void monthly_payment_standard_case() {
        assertThat(MoneyMath.monthlyPayment(new BigDecimal("1000.00"), new BigDecimal("0.12"), 12))
                .isEqualByComparingTo("88.85");
    }

    @Test
    void monthly_payment_zero_interest() {
        assertThat(MoneyMath.monthlyPayment(new BigDecimal("1200.00"), BigDecimal.ZERO, 12))
                .isEqualByComparingTo("100.00");
    }

    @Test
    void interest_portion_and_platform_fee() {
        BigDecimal interest = MoneyMath.interestPortion(new BigDecimal("1000.00"), new BigDecimal("0.12"));
        assertThat(interest).isEqualByComparingTo("10.00");
        assertThat(MoneyMath.platformFee(interest)).isEqualByComparingTo("0.10");
    }

    @Test
    void distribute_two_investors_is_exact() {
        List<BigDecimal> shares = MoneyMath.distribute(new BigDecimal("88.75"),
                List.of(new BigDecimal("600.00"), new BigDecimal("400.00")), new BigDecimal("1000.00"));

        assertThat(shares.get(0)).isEqualByComparingTo("53.25");
        assertThat(shares.get(1)).isEqualByComparingTo("35.50");
        assertThat(sum(shares)).isEqualByComparingTo("88.75");
    }

    @Test
    void distribute_residue_goes_to_largest_remainder_and_sums_exactly() {
        List<BigDecimal> shares = MoneyMath.distribute(new BigDecimal("100.00"),
                List.of(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE), new BigDecimal("3"));

        assertThat(sum(shares)).isEqualByComparingTo("100.00");
        assertThat(shares).contains(new BigDecimal("33.34"));
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
