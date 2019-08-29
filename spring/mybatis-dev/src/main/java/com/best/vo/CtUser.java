package com.best.vo;

import com.best.po.Book;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Param;

/**
 * @author dngzs
 * @date 2019-08-23 21:00
 */
public class CtUser {


    private Integer age;

    private Long id;

    private String username;

    private int sex;

    private Book book;

    public CtUser(Long id) {
        this.id = id;
    }

    public CtUser() {
    }

    public CtUser(Integer age) {
        this.age = age;
    }


    public CtUser(@Param("age")Integer age, @Param("id")Long id, @Param("username")String username) {
        this.age = age;
        this.id = id;
        this.username = username;
    }

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

    public int getSex() {
        return sex;
    }

    public void setSex(int sex) {
        this.sex = sex;
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
