package com.best.dao;

import com.best.po.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    User getById(@Param("id") Long id);

}
