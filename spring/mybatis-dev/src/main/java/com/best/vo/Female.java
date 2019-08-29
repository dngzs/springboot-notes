package com.best.vo;

import org.apache.ibatis.annotations.Param;

/**
 * 女
 */
public class Female extends CtUser{

    public Female(@Param("age")Integer age, @Param("id")Long id, @Param("username")String username) {
        super(age, id, username);
    }
}
