package com.best;

import com.best.service.ServiceA;
import com.best.service.ServiceB;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class App {

    public static void main(String[] args) {

        ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        //ServiceB bean = context.getBean(ServiceB.class);
        //bean.dosomethingB();

        ServiceA bean1 = context.getBean(ServiceA.class);
        bean1.dosomethingA();
    }

}
