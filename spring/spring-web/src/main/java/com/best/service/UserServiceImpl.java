package com.best.service;

import com.best.aop.AfterCommit;
import com.best.dao.UserDao;
import com.best.jdbc.MyRow;
import com.best.tx.SmsEvent;
import com.best.tx.SmsVo;
import com.best.vo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Arrays;
import java.util.List;


@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserDao userDao;
    @Autowired
    private UserSup userSup;
    @Autowired
    private ApplicationEventPublisher publisher;


    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void beforeCommit(SmsEvent<SmsVo> event) {
        SmsVo smsVo = (SmsVo) event.getSource();
        System.out.println("########################");
        System.out.println(smsVo);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @AfterCommit("test2")
    public List<User> test1(){
        List<User> list = userDao.findList();
        insert();
        SmsVo smsVo = new SmsVo();
        smsVo.setIds(Arrays.asList(1L));
        SmsEvent smsEvent = new SmsEvent(smsVo);
        publisher.publishEvent(smsEvent);
        return list;
    }

    public List<User> test2(){
        List<User> list = userDao.findList();
        insert();
        SmsVo smsVo = new SmsVo();
        smsVo.setIds(Arrays.asList(1L));
        SmsEvent smsEvent = new SmsEvent(smsVo);
        publisher.publishEvent(smsEvent);
        return list;
    }


    @Override
    public int count() {
        return userDao.count();
    }

    public void insert() {

        List<User> users = jdbcTemplate.query("SELECT * FROM ct_user", new MyRow());
        System.out.println(users);

        String sql = "insert into ct_user (username, age) values (?, ?)";
        jdbcTemplate.update(sql, "张三", 18);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void update() {
        userDao.update("张博8");
    }


   /* @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void update() {
        String sql2 = "update ct_user set username=? where id= 1";
        jdbcTemplate.update(sql2, "张博2");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert() {

        List<User> users = jdbcTemplate.query("SELECT * FROM ct_user", new MyRow());
        System.out.println(users);

        String sql = "insert into ct_user (username, age) values (?, ?)";
        jdbcTemplate.update(sql, "张三", 18);
    }*/


}
