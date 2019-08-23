package com.best.vo;

/**
 * @author dngzs
 * @date 2019-08-23 21:00
 */
public class CtUser {

    private Integer age;

    private Long id;

    private String username;


    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
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

    @Override
    public String toString() {
        return "User{" +
                "age=" + age +
                ", id=" + id +
                ", username='" + username + '\'' +
                '}';
    }
}
