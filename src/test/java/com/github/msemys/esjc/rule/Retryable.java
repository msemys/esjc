package com.github.msemys.esjc.rule;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Retryable {

    Class<? extends Throwable> value();

    int maxAttempts() default 3;

    long delay() default 1000;

}
