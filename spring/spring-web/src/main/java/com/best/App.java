package com.best;


import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class App
{
    public static void main( String[] args ){
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("spring.xml");
        ((ClassPathXmlApplicationContext) applicationContext).setAllowBeanDefinitionOverriding(false);
        //MathCalculator bean = applicationContext.getBean(MathCalculator.class);
       // bean.div(1,2);

        /*-------------------------------*/
        /*User bean = (User)applicationContext.getBean("user");
        System.out.println(bean.getCar());

        User bean3 = (User)applicationContext.getBean("user2");
        System.out.println(bean3.getCar());
        Config bean2 = applicationContext.getBean(Config.class);
        System.out.println(bean2);*/

        /*-------------------------------*/

        /*UserService bean3 = applicationContext.getBean(UserService.class);
        System.out.println(bean3.test1());*/
        com.best.service.A bean4 = applicationContext.getBean(com.best.service.A.class);
        bean4.b();
    }
}
