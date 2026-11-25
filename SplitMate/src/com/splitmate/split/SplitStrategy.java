package com.splitmate.split;

import java.math.BigDecimal;
import java.util.Map;

/** Interface (abstraction): every split type calculates shares the same way from the outside. */
public interface SplitStrategy {
    String getType();

    /**
     * @param total  total expense amount
     * @param inputs userId -> input value (ignored for EQUAL, amount for EXACT, percent for PERCENTAGE)
     * @return userId -> share amount (always sums exactly to total)
     */
    Map<Integer, BigDecimal> calculate(BigDecimal total, Map<Integer, BigDecimal> inputs);
}
