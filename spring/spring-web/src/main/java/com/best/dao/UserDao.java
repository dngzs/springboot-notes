package com.best.dao;


import com.best.vo.User;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserDao {

    List<User> findList();

    int update(@Param("username") String username);

    int insert();

    int count();
}
