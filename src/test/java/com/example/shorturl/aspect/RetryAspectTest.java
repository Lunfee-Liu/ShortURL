package com.example.shorturl.aspect;

import com.example.shorturl.annotation.Retryable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetryAspectTest {

    private RetryAspect aspect;

    @Mock
    private ProceedingJoinPoint pjp;

    @Mock
    private Signature signature;

    @BeforeEach
    void setUp() {
        aspect = new RetryAspect();
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("ShortUrlService.createShortUrl(..)");
    }

    // -----------------------------------------------------------------------
    // Helper: build a @Retryable annotation instance via a proxy method
    // -----------------------------------------------------------------------

    private Retryable retryable(Class<? extends Throwable>[] retryFor, int maxAttempts, long backoffMs)
            throws NoSuchMethodException {
        return new Retryable() {
            @Override public Class<? extends Throwable>[] retryFor()   { return retryFor; }
            @Override public int maxAttempts()                          { return maxAttempts; }
            @Override public long backoffMs()                           { return backoffMs; }
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return Retryable.class;
            }
        };
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    void succeedsOnFirstAttempt_noProceedCalledTwice() throws Throwable {
        when(pjp.proceed()).thenReturn("ok");

        @SuppressWarnings("unchecked")
        var ann = retryable(new Class[]{DuplicateKeyException.class}, 3, 0);
        Object result = aspect.retry(pjp, ann);

        assertThat(result).isEqualTo("ok");
        verify(pjp, times(1)).proceed();
    }

    @Test
    void retriesOnMatchingException_succeedsOnSecondAttempt() throws Throwable {
        when(pjp.proceed())
                .thenThrow(new DuplicateKeyException("dup"))
                .thenReturn("recovered");

        @SuppressWarnings("unchecked")
        var ann = retryable(new Class[]{DuplicateKeyException.class}, 3, 0);
        Object result = aspect.retry(pjp, ann);

        assertThat(result).isEqualTo("recovered");
        verify(pjp, times(2)).proceed();
    }

    @Test
    void exhaustsAllAttempts_throwsLastException() throws Throwable {
        var ex = new DuplicateKeyException("persistent dup");
        when(pjp.proceed()).thenThrow(ex);

        @SuppressWarnings("unchecked")
        var ann = retryable(new Class[]{DuplicateKeyException.class}, 3, 0);
        assertThatThrownBy(() -> aspect.retry(pjp, ann))
                .isSameAs(ex);
        verify(pjp, times(3)).proceed();
    }

    @Test
    void nonRetryableException_throwsImmediately() throws Throwable {
        var ex = new IllegalArgumentException("bad arg");
        when(pjp.proceed()).thenThrow(ex);

        @SuppressWarnings("unchecked")
        var ann = retryable(new Class[]{DuplicateKeyException.class}, 3, 0);
        assertThatThrownBy(() -> aspect.retry(pjp, ann))
                .isSameAs(ex);
        verify(pjp, times(1)).proceed();
    }

    @Test
    void emptyRetryFor_neverRetries() throws Throwable {
        var ex = new DuplicateKeyException("dup");
        when(pjp.proceed()).thenThrow(ex);

        @SuppressWarnings("unchecked")
        var ann = retryable(new Class[]{}, 3, 0);
        assertThatThrownBy(() -> aspect.retry(pjp, ann))
                .isSameAs(ex);
        verify(pjp, times(1)).proceed();
    }

    @Test
    void matchesCauseException() throws Throwable {
        // Spring sometimes wraps: outer exception wraps a DuplicateKeyException cause
        var cause = new DuplicateKeyException("dup");
        var wrapper = new RuntimeException("wrapped", cause);
        when(pjp.proceed())
                .thenThrow(wrapper)
                .thenReturn("ok");

        @SuppressWarnings("unchecked")
        var ann = retryable(new Class[]{DuplicateKeyException.class}, 3, 0);
        Object result = aspect.retry(pjp, ann);

        assertThat(result).isEqualTo("ok");
        verify(pjp, times(2)).proceed();
    }

    @Test
    void backoffMs_sleepsBetweenAttempts() throws Throwable {
        when(pjp.proceed())
                .thenThrow(new DuplicateKeyException("dup"))
                .thenReturn("ok");

        @SuppressWarnings("unchecked")
        var ann = retryable(new Class[]{DuplicateKeyException.class}, 3, 50);

        long start = System.currentTimeMillis();
        aspect.retry(pjp, ann);
        long elapsed = System.currentTimeMillis() - start;

        // At least one 50ms sleep must have occurred
        assertThat(elapsed).isGreaterThanOrEqualTo(50);
        verify(pjp, times(2)).proceed();
    }
}
