package com.best.configuration;

import com.best.vo.Car;
import com.best.vo.User;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Component
public class Config {


    @Bean
    public User user(){
        return new User(car());
    }

    @Bean
    public User user2(){
        return new User(car());
    }


    @Bean
    public Car car(){
        Car car = new Car();
        car.setName("baoma");
        return car;
    }
}
