package com.best.dependency;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author dngzs
 * @date 2019-06-25 11:06
 */
@Component
public class C {

    @Autowired
    private A a;
}
