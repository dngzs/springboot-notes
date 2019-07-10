package com.best.aop;

import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * @author dngzs
 * @date 2019-05-31 14:44
 */
public abstract class AfterCommitPointcut extends StaticMethodMatcherPointcut implements Serializable {

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        return  method.isAnnotationPresent(AfterCommit.class);
    }



    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof AfterCommitPointcut)) {
            return false;
        }
        AfterCommitPointcut otherPc = (AfterCommitPointcut) other;
        return ObjectUtils.nullSafeEquals(getAfterCommitSource(), otherPc.getAfterCommitSource());
    }

    @Override
    public int hashCode() {
        return AfterCommitPointcut.class.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + getAfterCommitSource();
    }


    /**
     * Obtain the underlying TransactionAttributeSource (may be {@code null}).
     * To be implemented by subclasses.
     */
    protected abstract AfterCommitSource getAfterCommitSource();

}
