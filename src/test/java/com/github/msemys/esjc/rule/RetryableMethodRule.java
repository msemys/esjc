package com.github.msemys.esjc.rule;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.msemys.esjc.util.Threads.sleepUninterruptibly;

public class RetryableMethodRule implements MethodRule {
    private static final Logger logger = LoggerFactory.getLogger(RetryableMethodRule.class);

    @Override
    public Statement apply(Statement base, FrameworkMethod method, Object target) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                Retryable retryable = method.getAnnotation(Retryable.class);

                if (retryable != null) {
                    for (int i = 1; i <= retryable.maxAttempts(); i++) {
                        try {
                            base.evaluate();
                            break;
                        } catch (Throwable t) {
                            if (retryable.value().isAssignableFrom(t.getClass())) {
                                logger.warn("Attempt {}/{} failed.", i, retryable.maxAttempts(), t);

                                if (i == retryable.maxAttempts()) {
                                    throw t;
                                } else {
                                    if (retryable.delay() > 0) {
                                        sleepUninterruptibly(retryable.delay());
                                    }
                                }
                            } else {
                                throw t;
                            }
                        }
                    }
                } else {
                    base.evaluate();
                }
            }
        };
    }

}
