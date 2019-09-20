package com.best;

import com.best.dao.BookMapper;
import com.best.dao.UserMapper;
import com.best.po.Book;
import com.best.po.User;
import com.best.vo.CtUser;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.SqlSessionManager;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class App {

    public static void main(String[] args) throws Exception{

        InputStream inputStream = Resources.getResourceAsStream("config-mybatis.xml");
        SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        SqlSessionFactory sqlSessionFactory = builder.build(inputStream);
        SqlSession sqlSession = sqlSessionFactory.openSession();
        UserMapper mapper = sqlSession.getMapper(UserMapper.class);
        /*User query = new User();
        query.setAge(18);
        query.setId(1L);
        CtUser user = mapper.getWithAgeAndId(1l,0);
        System.out.println(user);*/
        List ids = new ArrayList();
        ids.add(1L);
        ids.add(2L);
        CtUser byId = mapper.getById(null);

        BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);
        Book book = bookMapper.getById(1L);
        System.out.println(book);
    }
}
