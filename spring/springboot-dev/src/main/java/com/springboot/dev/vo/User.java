package com.springboot.dev.vo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class User {

    private Long id;

    private String username;

    private Date bith;

    private Integer age;

    private Car car;

    private List<String> carNames = new ArrayList<>();


    public User() {
    }

    public User(Car car) {
        this.car = car;
    }

    public User(String username, Date bith, Integer age) {
        this.username = username;
        this.bith = bith;
        this.age = age;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Date getBith() {
        return bith;
    }

    public void setBith(Date bith) {
        this.bith = bith;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Car getCar() {
        return car;
    }

    public void setCar(Car car) {
        this.car = car;
    }

    public List<String> getCarNames() {
        return carNames;
    }

    public void setCarNames(List<String> carNames) {
        this.carNames = carNames;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", bith=" + bith +
                ", age=" + age +
                ", car=" + car +
                ", carNames=" + carNames +
                '}';
    }
}
