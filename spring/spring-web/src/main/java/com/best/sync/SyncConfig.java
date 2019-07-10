package com.best.sync;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;


@Configuration
@EnableAsync
public class SyncConfig {

    @Resource
    private AsyncAnnotationBeanPostProcessor asyncAdvisor;

    @PostConstruct
    private void init(){
        asyncAdvisor.setExposeProxy(true);
    }
}
