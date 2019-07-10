package com.best.jdbc;


import com.best.vo.User;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 实现RowMapper接口，返回User对象
 * */
public class MyRow implements RowMapper<User>{

    @Override
    public User mapRow(ResultSet resultSet, int i) throws SQLException {
        String name = resultSet.getString("username");
        Integer age = resultSet.getInt("age");
        Long id = resultSet.getLong("id");
        User user = new User();
        user.setUsername(name);
        user.setAge(age);
        user.setId(id);
        return user;
    }
}
