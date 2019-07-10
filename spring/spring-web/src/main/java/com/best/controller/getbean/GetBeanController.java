package com.best.controller.getbean;

import com.best.getbean.ApplicationContextGetBean;
import com.best.getbean.ApplicationObjectSupportGetBean;
import com.best.getbean.WebApplicationContextUtilsGetBean;
import com.best.getbean.WebApplicationObjectSupportGetBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

/**
 * 获取bean的方式
 *
 * @author dngzs
 * @date 2019-05-16 15:11
 */
@Slf4j
@RestController
public class GetBeanController {

    @Autowired
    private ApplicationContextGetBean applicationContextGetBean;

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private WebApplicationContextUtilsGetBean webApplicationContextUtilsGetBean;
    @Autowired
    private ApplicationObjectSupportGetBean applicationObjectSupportGetBean;
    @Autowired
    private WebApplicationObjectSupportGetBean webApplicationObjectSupportGetBean;

    @RequestMapping("applicationGetBean1")
    public String applicationGetBean1(){
        CommonsMultipartResolver multipartResolver = (CommonsMultipartResolver)applicationContext.getBean("multipartResolver");
        System.out.println(multipartResolver != null);
        System.out.println(multipartResolver);
        applicationContextGetBean.applicationGetBean();
        return "success";
    }

    @RequestMapping("applicationGetBean2")
    public String applicationGetBean2(){
        webApplicationContextUtilsGetBean.getBean();
        return "success";
    }

    @RequestMapping("applicationGetBean3")
    public String applicationGetBean3(){
        applicationObjectSupportGetBean.getBean();
        return "success";
    }

    @RequestMapping("applicationGetBean4")
    public String applicationGetBean4(){
        webApplicationObjectSupportGetBean.getBean();
        return "success";
    }



}
