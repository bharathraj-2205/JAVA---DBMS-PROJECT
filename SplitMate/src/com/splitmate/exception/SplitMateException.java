package com.splitmate.exception;

/** Custom unchecked exception for business-rule violations. */
public class SplitMateException extends RuntimeException {
    public SplitMateException(String message) { super(message); }
    public SplitMateException(String message, Throwable cause) { super(message, cause); }
}
