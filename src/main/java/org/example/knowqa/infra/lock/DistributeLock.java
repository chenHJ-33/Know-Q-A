package org.example.knowqa.infra.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributeLock {
    String scene();
    String key() default DistributeLockConstant.NONE_KEY;
    String keyExpression() default DistributeLockConstant.NONE_KEY;
    int expireTime() default DistributeLockConstant.DEFAULT_EXPIRE_TIME;
    int waitTime() default DistributeLockConstant.DEFAULT_WAIT_TIME;
}
