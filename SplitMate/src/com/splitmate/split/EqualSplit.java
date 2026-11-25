package com.splitmate.split;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

public class EqualSplit extends AbstractSplit {
    @Override public String getType() { return "EQUAL"; }

    @Override
    public Map<Integer, BigDecimal> calculate(BigDecimal total, Map<Integer, BigDecimal> inputs) {
        requireParticipants(inputs);
        BigDecimal each = total.divide(BigDecimal.valueOf(inputs.size()), 2, RoundingMode.DOWN);
        Map<Integer, BigDecimal> shares = new LinkedHashMap<>();
        for (Integer userId : inputs.keySet()) shares.put(userId, each);
        fixRounding(total, shares);
        return shares;
    }
}
