package com.springboot.dev.tools;

import com.springboot.dev.vo.Car;
import com.springboot.dev.vo.User;

import java.util.ArrayList;
import java.util.List;

public class List2Array {

    public static void main(String[] args) {
        List<String> list = new ArrayList<>();
        list.add("张三");
        list.add("王五");
        list.add("李留");
        list.add("liwu");

        String[] strings = list.toArray(new String[0]);
        for (String string : strings) {
            System.out.println(string);
        }

        List<User> users = new ArrayList<>();
        User user1 =new User();
        user1.setAge(18);
        user1.setUsername("zhangsan");
        Car car1 =new Car();
        car1.setName("baoma");
        user1.setCar(car1);


        User user2 =new User();
        user2.setAge(19);
        user2.setUsername("李四");
        Car car2 =new Car();
        car2.setName("benchi");
        user2.setCar(car2);
        users.add(user2);
        users.add(user1);
        User[] users1 = users.toArray(new User[0]);
        for (User user : users1) {
            System.out.println(user);
        }
    }
}
