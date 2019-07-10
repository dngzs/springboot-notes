package com.best.aop;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义日志注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SystemLog {

    /**
     * 日志作用域
     * @return
     */
    String name() default "";

    /**
     * 日志操作描述
     * @return
     */
    String value() default "";
}
