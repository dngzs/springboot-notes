package com.best.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceBImpl implements ServiceB {

    @Autowired
    private ServiceA serviceA;

    @Transactional
    public void dosomethingB() {
        try{
            serviceA.dosomethingA();

        }catch(Exception e){
            System.err.println("错误了");
        }
    }
}
