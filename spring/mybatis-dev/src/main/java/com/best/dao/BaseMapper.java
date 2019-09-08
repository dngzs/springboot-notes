package com.best.dao;

import org.apache.ibatis.annotations.Param;

public interface BaseMapper<T> {

    T getById(Long id);
}
