package com.best.dao;

import com.best.po.Book;
import org.apache.ibatis.annotations.Param;

public interface BookMapper {
    Book getById(@Param("id") Long id);
}