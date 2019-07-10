package com.best.getbean;

import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.ServletContext;

/**
 * 获取bean工厂的第二种方式
 *
 * @author dngzs
 * @date 2019-05-17 14:41
 */
@Component
public class WebApplicationContextUtilsGetBean  {


    public void getBean(){
        ServletContext servletContext = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession().getServletContext();
        WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        WebApplicationContext requiredWebApplicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(servletContext);
        System.out.println(webApplicationContext);
        System.out.println(requiredWebApplicationContext);
    }
}
