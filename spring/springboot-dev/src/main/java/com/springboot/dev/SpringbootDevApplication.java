package com.springboot.dev;

import com.springboot.dev.vo.Car;
import com.springboot.dev.vo.User;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
public class SpringbootDevApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SpringbootDevApplication.class, args);
        User user1 = (User)context.getBean("user");
        User user2 = (User)context.getBean("user2");
        Car car = (Car)context.getBean("car");
        System.out.println(user1.getCar());
        System.out.println(user2.getCar());
        System.out.println(car);
    }

}
