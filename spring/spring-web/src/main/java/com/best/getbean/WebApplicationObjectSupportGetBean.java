package com.best.getbean;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationObjectSupport;

import javax.servlet.ServletContext;

/**
 * 获取bean的第四种方式
 *
 * @author dngzs
 * @date 2019-05-17 15:13
 */
@Component
public class WebApplicationObjectSupportGetBean extends WebApplicationObjectSupport {

    private ServletContext servletContext;

    @Override
    protected void initServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void getBean(){
        ApplicationContext applicationContext = this.getApplicationContext();
        System.out.println(applicationContext);
        System.out.println(servletContext);
        Object attribute = servletContext.getAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
        System.out.println(attribute);
    }
}
