package com.best.dependency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author dngzs
 * @date 2019-06-25 11:05
 */
@Component
public class A {

    private B b;


    @Autowired
    public void setB(B b) {
        this.b = b;
    }

    public B getB() {
        return b;
    }

}
