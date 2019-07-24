package com.best.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * @author dngzs
 * @date 2019-07-24 18:04
 */
@Aspect
public class DogAspect {
    @Around("execution(public void com.best.Dog.*(..))")
    public Object aspect(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("before run.");
        Object result = joinPoint.proceed();
        System.out.println("after run.");
        return result;
    }
}
