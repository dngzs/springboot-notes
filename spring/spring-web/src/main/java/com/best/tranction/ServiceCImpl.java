package com.best.tranction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ServiceCImpl implements ServiceC {

    @Autowired
    private ServiceB serviceB;

    @Override
    @Transactional
    public void dosomethingC() {
        serviceB.dosomethingB();
    }
}
