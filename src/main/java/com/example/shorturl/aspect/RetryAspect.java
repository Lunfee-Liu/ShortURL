package com.example.shorturl.aspect;

import com.example.shorturl.annotation.Retryable;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that provides automatic retry for methods annotated with {@link Retryable}.
 *
 * <p><b>Ordering guarantee:</b> {@code @Order(Ordered.LOWEST_PRECEDENCE - 1)} makes this
 * aspect run <em>outside</em> {@code @Transactional} (which defaults to
 * {@code Ordered.LOWEST_PRECEDENCE}). The proxy chain is therefore:
 * <pre>
 *   Caller → RetryAspect → TransactionProxy → actual method
 * </pre>
 * On a retryable exception the transaction is fully rolled back before the next
 * attempt starts a fresh one — preventing partial state from leaking between tries.
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class RetryAspect {

    @Around("@annotation(retryable)")
    public Object retry(ProceedingJoinPoint pjp, Retryable retryable) throws Throwable {
        int maxAttempts = retryable.maxAttempts();
        Throwable lastEx = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return pjp.proceed();
            } catch (Throwable ex) {
                if (!isRetryable(ex, retryable.retryFor())) {
                    throw ex;
                }
                lastEx = ex;
                log.warn("retryable error on attempt {}/{}, method={}, cause={}",
                        attempt, maxAttempts,
                        pjp.getSignature().toShortString(),
                        ex.getMessage());
                if (attempt < maxAttempts && retryable.backoffMs() > 0) {
                    Thread.sleep(retryable.backoffMs());
                }
            }
        }

        throw lastEx;
    }

    /**
     * Returns true if {@code ex} or its direct cause is an instance of any type in
     * {@code retryFor}.  Checking the cause handles cases where Spring/MyBatis wraps
     * the original exception (e.g. {@code DuplicateKeyException} wrapping
     * {@code SQLIntegrityConstraintViolationException}).
     */
    private boolean isRetryable(Throwable ex, Class<? extends Throwable>[] retryFor) {
        for (Class<? extends Throwable> type : retryFor) {
            if (type.isInstance(ex) || (ex.getCause() != null && type.isInstance(ex.getCause()))) {
                return true;
            }
        }
        return false;
    }
}
