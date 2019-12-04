package com.springboot.dev;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = "user.id=8")
@SpringBootTest(properties = "user.id=7",webEnvironment = SpringBootTest.WebEnvironment.NONE )
public class SpringbootDevApplicationTests {

    @Value("${user.id}")
    private Long userId;

    @Test
    public void contextLoads() {
        Assert.assertEquals(userId.longValue(),8);
    }

}
