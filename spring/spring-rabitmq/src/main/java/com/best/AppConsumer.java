package com.best;

import com.best.consumer.MessageConsumer;

/**
 * @author dngzs
 * @date 2019-09-17 11:04
 */
public class AppConsumer {

    //这个队列名字要和生产者中的名字一样，否则找不到队列
    private final static String QUEUE_NAME = "myQueue";

    public static void main( String[] args )
    {
        MessageConsumer consumer = new MessageConsumer();
        consumer.consume(QUEUE_NAME);
    }
}
