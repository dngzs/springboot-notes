package com.best.aop;

import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;


/**
 * @author dngzs
 * @date 2019-05-31 14:23
 */
@Aspect
@Component
public class MyAdvice implements org.aopalliance.intercept.MethodInterceptor {

    @Override
    public Object invoke(final MethodInvocation invocation) throws Throwable {
        Object proceed = invocation.proceed();
        System.out.println("-----------------");
        return proceed;


    }

}
