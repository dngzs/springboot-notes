package com.best;


import com.best.scope.Scope2;
import com.best.service.UserService;
import com.best.tranction.ServiceB;
import com.best.tranction.ServiceC;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class App
{
    public static void main( String[] args ){
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring.xml");
        ((ClassPathXmlApplicationContext) applicationContext).setAllowBeanDefinitionOverriding(false);
        //MathCalculator bean = applicationContext.getBean(MathCalculator.class);
        //bean.div(1,2);

        /*-------------------------------*/
        /*User bean = (User)applicationContext.getBean("user");
        System.out.println(bean.getCar());

        User bean3 = (User)applicationContext.getBean("user2");
        System.out.println(bean3.getCar());
        Config bean2 = applicationContext.getBean(Config.class);
        System.out.println(bean2);*/

        /*-------------------------------*/
        Scope2 bean = applicationContext.getBean(Scope2.class);
        bean.dosome();
        bean.dosome();
        //ServiceC bean3 = applicationContext.getBean(ServiceC.class);
        //bean3.dosomethingC();
        //com.best.service.A bean4 = applicationContext.getBean(com.best.service.A.class);
        //bean4.b();
    }
}
