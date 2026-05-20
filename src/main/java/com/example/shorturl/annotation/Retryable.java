package com.example.shorturl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method for automatic retry on specified exceptions.
 *
 * <p>The retry aspect wraps <em>outside</em> {@code @Transactional}, so each
 * attempt runs in its own transaction — ensuring proper rollback and a fresh
 * database state before the next try.
 *
 * <p>Usage example:
 * <pre>{@code
 * @Retryable(retryFor = DuplicateKeyException.class, maxAttempts = 3)
 * @Transactional(rollbackFor = Exception.class)
 * public Foo doWork() { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Retryable {

    /**
     * Exception types that should trigger a retry.
     * Both the thrown exception and its direct cause are checked.
     * Defaults to empty — no exception triggers a retry.
     */
    Class<? extends Throwable>[] retryFor() default {};

    /**
     * Maximum number of attempts, including the first try.
     * Must be >= 1. Defaults to 3 (1 original + 2 retries).
     */
    int maxAttempts() default 3;

    /**
     * Milliseconds to wait between attempts. 0 means no delay.
     * Applied only between attempts, not before the first one.
     */
    long backoffMs() default 0;
}
