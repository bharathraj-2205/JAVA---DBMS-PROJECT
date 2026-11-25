package com.splitmate.split;

import com.splitmate.exception.SplitMateException;
import java.math.BigDecimal;
import java.util.Map;

/** Shared logic for all strategies (inheritance): rounding fix and validation. */
public abstract class AbstractSplit implements SplitStrategy {

    protected void requireParticipants(Map<Integer, BigDecimal> inputs) {
        if (inputs == null || inputs.isEmpty())
            throw new SplitMateException("At least one participant is required.");
    }

    /** Pushes any leftover paise caused by rounding onto the first participant. */
    protected void fixRounding(BigDecimal total, Map<Integer, BigDecimal> shares) {
        BigDecimal sum = shares.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal diff = total.subtract(sum);
        if (diff.signum() != 0) {
            Integer first = shares.keySet().iterator().next();
            shares.put(first, shares.get(first).add(diff));
        }
    }
}
