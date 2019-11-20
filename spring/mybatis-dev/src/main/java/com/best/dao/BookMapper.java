package com.best.dao;

import com.best.po.Book;
import com.best.po.User;
import org.apache.ibatis.annotations.Param;

public interface BookMapper {
    Book getById(Book book );

    Book getByIdAndName(Long id,Long userId);
}
