package com.best.spring;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author dngzs
 * @date 2019-09-17 12:47
 */
@Component
public class ConsumerListener2 implements ChannelAwareMessageListener{

    @Autowired
    private MessageConverter msgConverter;

    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        channel.basicQos(1);
        String s = (String) msgConverter.fromMessage(message);
        System.out.println("-------------------"+s);
        //Thread.sleep(100000l);
        throw  new RuntimeException("error");

    }
}
