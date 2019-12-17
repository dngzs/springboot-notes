package com.springboot.dev.tools;

import com.springboot.dev.dao.BaseDao;
import com.springboot.dev.dao.UserDao;
import com.springboot.dev.vo.User;
import org.springframework.core.ResolvableType;

import java.lang.reflect.Type;

public class ResolvableTypeDemo {

    public static void main(String[] args) {

        ResolvableType resolvableType = ResolvableType.forClass(UserDao.class);
        ResolvableType generic = resolvableType.getGeneric(0);
        ResolvableType as = resolvableType.as(BaseDao.class);
        System.out.println(as.getGeneric(0));
    }
}
