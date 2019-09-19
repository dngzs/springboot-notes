package com.best;

import com.best.product.MessageSender;

/**
 * @author dngzs
 * @date 2019-09-17 10:56
 */
public class App {
    public static void main(String[] args) {
        MessageSender sender = new MessageSender();
        sender.sendMessage("hello RabbitMQ!");
    }
}