package com.springboot.dev.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.springboot.dev.anno.SpringAnno;
import com.springboot.dev.vo.Car;
import com.springboot.dev.vo.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;


@SpringAnno
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
