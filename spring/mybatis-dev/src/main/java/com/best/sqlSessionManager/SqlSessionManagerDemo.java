package com.best.sqlSessionManager;

import com.best.dao.UserMapper;
import com.best.po.User;
import com.best.vo.CtUser;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSessionManager;

import java.io.InputStream;

/**
 * @author dngzs
 * @date 2019-08-22 15:53
 */
public class SqlSessionManagerDemo {

    public static void main(String[] args)throws Exception {
        InputStream inputStream = Resources.getResourceAsStream("config-mybatis.xml");
        SqlSessionManager sqlSessionManager = SqlSessionManager.newInstance(inputStream);
        sqlSessionManager.startManagedSession();
        //sqlSessionManager.startManagedSession(false); 加自动提交false和不加没什么区别，不加就默认false
        UserMapper mapper = sqlSessionManager.getMapper(UserMapper.class);

        try {
            CtUser user1 = mapper.getById(1l);
            CtUser user2 = mapper.getWithAgeAndId(1l,15);
            sqlSessionManager.commit();
        } catch (Exception e) {
            sqlSessionManager.rollback();
        } finally {
            sqlSessionManager.close();
        }

    }
}
