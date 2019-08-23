package com.best.aop;

import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;


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
        System.out.println("##########事务已提交#############");
        AfterCommit afterCommit = invocation.getMethod().getAnnotation(AfterCommit.class);
        Class<?> declaringClass = invocation.getMethod().getDeclaringClass();
        Method method = declaringClass.getMethod(afterCommit.value(),invocation.getMethod().getParameterTypes());
        method.invoke(invocation.getThis(), invocation.getArguments());
        return proceed;

    }

}
