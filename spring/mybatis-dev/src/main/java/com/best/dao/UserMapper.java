package com.best.dao;

import com.best.po.User;
import com.best.vo.CtUser;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    CtUser getById(@Param("id") Long id);

    CtUser getWithAgeAndId(@Param("id")Long id,@Param("age")Integer age);

    CtUser getWithPo(User user);


}
