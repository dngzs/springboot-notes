package com.best;

import com.best.dao.UserMapper;
import com.best.po.User;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;

public class App {

    public static void main(String[] args) throws Exception{
        InputStream inputStream = Resources.getResourceAsStream("config-mybatis.xml");
        SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        SqlSessionFactory sqlSessionFactory = builder.build(inputStream);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        User query = new User();
        query.setAge(18);
        query.setId(1L);
        User user = mapper.getWithPo(query);
        System.out.println(user);
    }
}
