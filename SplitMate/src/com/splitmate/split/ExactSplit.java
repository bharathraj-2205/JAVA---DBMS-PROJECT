package com.splitmate.split;

import com.splitmate.exception.SplitMateException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExactSplit extends AbstractSplit {
    @Override public String getType() { return "EXACT"; }

    @Override
    public Map<Integer, BigDecimal> calculate(BigDecimal total, Map<Integer, BigDecimal> inputs) {
        requireParticipants(inputs);
        Map<Integer, BigDecimal> shares = new LinkedHashMap<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (Map.Entry<Integer, BigDecimal> e : inputs.entrySet()) {
            BigDecimal v = e.getValue().setScale(2, RoundingMode.HALF_UP);
            shares.put(e.getKey(), v);
            sum = sum.add(v);
        }
        if (sum.compareTo(total) != 0)
            throw new SplitMateException("Exact amounts add up to " + sum + " but the expense is " + total + ".");
        return shares;
    }
}
