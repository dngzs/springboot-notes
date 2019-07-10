package com.best.aop;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;
import org.springframework.core.annotation.Order;

/**
 * @author dngzs
 * @date 2019-05-31 14:41
 */
@Order(0)
public class MyAdvisor extends AbstractBeanFactoryPointcutAdvisor  {

    private AfterCommitSource afterCommitSource;

    private final AfterCommitPointcut pointcut = new AfterCommitPointcut() {
        @Override
        protected AfterCommitSource getAfterCommitSource() {
            return afterCommitSource;
        }
    };

    public void setAfterCommitSource(AfterCommitSource afterCommitSource) {
        this.afterCommitSource = afterCommitSource;
    }
    /**
     * Set the {@link ClassFilter} to use for this pointcut.
     * Default is {@link ClassFilter#TRUE}.
     */
    public void setClassFilter(ClassFilter classFilter) {
        this.pointcut.setClassFilter(classFilter);
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }



}
