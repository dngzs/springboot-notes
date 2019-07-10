package com.best.PropertyAccessor;

import com.best.vo.User;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.ConfigurablePropertyAccessor;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.PropertyAccessorFactory;

import java.util.Date;

public class PropertyAccessorDemo {

    public static void main(String[] args) {
        User user = new User("zhangbo", new Date(), 19);
        //BeanWrapper
        BeanWrapper beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(user);
        Class<?> age = beanWrapper.getPropertyType("age");
        System.out.println(age);
        Object bith = beanWrapper.getPropertyValue("bith");
        System.out.println(bith);
        //DirectFieldAccessor
        ConfigurablePropertyAccessor configurablePropertyAccessor = PropertyAccessorFactory.forDirectFieldAccess(user);
        Object age1 = configurablePropertyAccessor.getPropertyValue("age");
        configurablePropertyAccessor.setPropertyValue("age","20");
        System.out.println(age1);

        DirectFieldAccessor directFieldAccessor = new DirectFieldAccessor(user);
        System.out.println(directFieldAccessor.getPropertyValue("age"));
    }
}
