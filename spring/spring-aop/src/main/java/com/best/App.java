package com.best;

import com.best.asm.Bc;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author dngzs
 * @date 2019-07-24 18:08
 */
public class App {
    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml");
        Dog dog = context.getBean(Dog.class);
        dog.run();
        Bc bc = new Bc();
    }
}
