package com.best.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 日志切面
 *
 * @author BG349176
 * @date 2019/2/28
 */
@Slf4j
@Aspect
@Component
public class LogAspect {

    /**
     * 如果请求时间超过3000ms，则将日志设置为警告级别
     */
    private static final int TO_WARNING_TIME = 3000;

    /**
     * 记录所有的请求日志
     *
     * @param jonPoint
     * @return
     * @throws Throwable
     */
    @AfterReturning(value = "@annotation(com.best.aop.SystemLog)", returning = "result")
    public Object loggerForControllerRequest(JoinPoint jonPoint, Object result) throws Throwable {

        return result;
    }



}
