package com.best.vo;

import org.apache.ibatis.annotations.Param;

public class Male extends CtUser {

    public Male(@Param("age")Integer age, @Param("id")Long id, @Param("username")String username) {
        super(age, id, username);
    }
}
