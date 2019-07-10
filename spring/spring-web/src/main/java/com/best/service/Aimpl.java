package com.best.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * @author dngzs
 * @date 2019-06-25 18:41
 */
@Component
@Slf4j
public class Aimpl implements A {

    @Resource
    private AsyncAnnotationBeanPostProcessor asyncAdvisor;

    @PostConstruct
    private void init(){
        asyncAdvisor.setExposeProxy(true);
    }

    @Override
    public void b() {
        ((Aimpl)AopContext.currentProxy()).c();
    }

    @Async
    public void c() {
        System.out.println(Thread.currentThread().getName()+"---------------");
    }
}
