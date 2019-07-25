package com.best.targetsource;

import com.best.Dog;
import org.springframework.aop.TargetSource;

/**
 * @author dngzs
 * @date 2019-07-25 15:51
 */
public class DogTargetSource implements TargetSource {
    public Class<?> getTargetClass() {
        return Dog.class;
    }

    public boolean isStatic() {
        return true;
    }

    public Object getTarget() throws Exception {
        return new Dog();
    }

    public void releaseTarget(Object target) throws Exception {

    }
}
