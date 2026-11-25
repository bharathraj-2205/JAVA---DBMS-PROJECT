package com.splitmate.split;

import com.splitmate.exception.SplitMateException;

/** Returns the right strategy at runtime (runtime polymorphism). */
public final class SplitFactory {
    private SplitFactory() { }

    public static SplitStrategy forType(String type) {
        switch (type.toUpperCase()) {
            case "EQUAL":      return new EqualSplit();
            case "EXACT":      return new ExactSplit();
            case "PERCENTAGE": return new PercentageSplit();
            default: throw new SplitMateException("Unknown split type: " + type);
        }
    }
}
