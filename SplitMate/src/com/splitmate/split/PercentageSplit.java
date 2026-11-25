package com.splitmate.split;

import com.splitmate.exception.SplitMateException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

public class PercentageSplit extends AbstractSplit {
    @Override public String getType() { return "PERCENTAGE"; }

    @Override
    public Map<Integer, BigDecimal> calculate(BigDecimal total, Map<Integer, BigDecimal> inputs) {
        requireParticipants(inputs);
        BigDecimal percentSum = inputs.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        if (percentSum.compareTo(BigDecimal.valueOf(100)) != 0)
            throw new SplitMateException("Percentages add up to " + percentSum + " but must be exactly 100.");
        Map<Integer, BigDecimal> shares = new LinkedHashMap<>();
        for (Map.Entry<Integer, BigDecimal> e : inputs.entrySet()) {
            shares.put(e.getKey(), total.multiply(e.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN));
        }
        fixRounding(total, shares);
        return shares;
    }
}
