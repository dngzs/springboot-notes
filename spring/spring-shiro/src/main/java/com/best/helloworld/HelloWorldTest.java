package com.best.helloworld;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.SimpleAccountRealm;
import org.apache.shiro.subject.Subject;
import org.junit.Assert;
import org.junit.Test;

/**
 * shiro第一个hello world
 *
 * @author dngzs
 * @date 2019-11-28 19:30
 */
public class HelloWorldTest {


    @Test
    public void test(){
        //创建SecurityManager
        DefaultSecurityManager defaultSecurityManager = new DefaultSecurityManager();

        //绑定SecurityManager给SecurityUtils
        SecurityUtils.setSecurityManager(defaultSecurityManager);

        //new一个简单的realm
        SimpleAccountRealm simpleAccountRealm = new SimpleAccountRealm();
        simpleAccountRealm.addAccount("zhangbo","123");

        //将Realm注册到SecurityManager
        defaultSecurityManager.setRealm(simpleAccountRealm);

        //获取主体Subject
        Subject subject = SecurityUtils.getSubject();
        UsernamePasswordToken token = new UsernamePasswordToken("zhangbo", "123");

        //登录，认证身份
        subject.login(token);

        //用户是否已登录
        Assert.assertTrue(subject.isAuthenticated());

        //退出登录
        subject.logout();
    }
}
