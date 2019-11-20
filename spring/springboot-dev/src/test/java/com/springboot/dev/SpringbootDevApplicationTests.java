package com.springboot.dev;

import com.springboot.dev.bean.Bean1;
import com.springboot.dev.config.Config;
import com.springboot.dev.controller.IndexController;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = "user.id=8")
@SpringBootTest(properties = "user.id=7")
public class SpringbootDevApplicationTests {

    @Value("${user.id}")
    private Long userId;

    @Test
    public void contextLoads() {
        Assert.assertEquals(userId.longValue(),8);
    }

}
