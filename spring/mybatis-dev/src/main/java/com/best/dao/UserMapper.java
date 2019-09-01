package com.best.dao;

import com.best.po.User;
import com.best.vo.CtUser;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserMapper {

    CtUser getById(@Param("id") Long id);

    List<CtUser> getByIds(@Param("ids") List ids);


    CtUser getWithAgeAndId(@Param("id")Long id,@Param("age")Integer age);

    CtUser getWithPo(User user);


}
