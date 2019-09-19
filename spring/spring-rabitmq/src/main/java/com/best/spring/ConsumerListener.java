package com.best.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author dngzs
 * @date 2019-09-17 12:16
 */
public class ConsumerListener {

    Logger logger = LoggerFactory.getLogger(ConsumerListener.class);

    public void listen(String message) throws InterruptedException {
        Thread.sleep(1000000L);
        logger.debug("received:"+message);
    }
}
