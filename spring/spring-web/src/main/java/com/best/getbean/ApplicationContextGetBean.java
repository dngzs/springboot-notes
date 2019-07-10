package com.best.getbean;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

/**
 * 获取bean的五种方式第一种
 *
 * @author dngzs
 * @date 2019-05-15 20:25
 */
@Component
@Slf4j
public class ApplicationContextGetBean {

    @Autowired
    private ApplicationContext applicationContext;

    public void applicationGetBean(){
        CommonsMultipartResolver multipartResolver = (CommonsMultipartResolver)applicationContext.getBean("multipartResolver");
        System.out.println(multipartResolver != null);
        System.out.println(multipartResolver);
    }
    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
}
