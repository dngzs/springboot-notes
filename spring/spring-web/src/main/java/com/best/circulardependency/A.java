package com.best.circulardependency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author dngzs
 * @date 2019-06-25 11:05
 */
@Component
public class A {

    @Autowired
    private B b;
}
