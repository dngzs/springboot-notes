package com.best.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service

public class ServiceAImpl implements ServiceA {

    @Transactional(propagation=Propagation.NESTED)
    public void dosomethingA(){
        int i = 1/0;
    }

}
