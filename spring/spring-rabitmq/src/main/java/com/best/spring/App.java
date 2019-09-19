package com.best.spring;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author dngzs
 * @date 2019-09-17 12:23
 */
public class App {

    public static void main(String[] args) throws InterruptedException {
        //启动Spring环境
        AbstractApplicationContext ctx = new ClassPathXmlApplicationContext("applicatioContext.xml");
        //假装是Autowired的
        RabbitTemplate template = ctx.getBean(RabbitTemplate.class);
        //设置routingKey
        template.setRoutingKey("foo.bar");
        //发送，exchange，routingKey什么的都配好了
        template.convertAndSend("Hello, world1!");
        template.convertAndSend("Hello, world2!");
        template.convertAndSend("Hello, world3!");
        template.convertAndSend("Hello, world4!");
        template.convertAndSend("Hello, world!5");
        //关掉环境
        Thread.sleep(10000000);
        //ctx.destroy();
    }
}
