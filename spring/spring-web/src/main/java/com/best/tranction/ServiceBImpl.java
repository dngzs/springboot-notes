package com.best.tranction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceBImpl implements ServiceB {

    @Autowired
    private ServiceA serviceA;

    @Transactional(propagation =Propagation.REQUIRES_NEW)
    public void dosomethingB() {
        serviceA.dosomethingA();
    }
}
