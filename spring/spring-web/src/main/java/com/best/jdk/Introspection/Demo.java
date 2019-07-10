package com.best.jdk.Introspection;

import com.best.vo.User;
import org.apache.commons.beanutils.BeanUtils;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

public class Demo {

    public static void main(String[] args) throws Exception {
        User user = new User();
        user.setUsername("zhangbo");
        PropertyDescriptor propertyDescriptor = new PropertyDescriptor("username", User.class);
        //获取set方法
        Method writeMethod = propertyDescriptor.getWriteMethod();
        Object zhangsan = writeMethod.invoke(user, "zhangsan");
        Method readMethod = propertyDescriptor.getReadMethod();
        Object invoke = readMethod.invoke(user);
        System.out.println(invoke);


        BeanInfo beanInfo = Introspector.getBeanInfo(User.class);
        PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
            if( "age".equals(pd.getName())){
                Method writeMethod1 = pd.getWriteMethod();
                writeMethod1.invoke(user, 80);
                Method readMethod1 = pd.getReadMethod();
                System.out.println(readMethod1.invoke(user));
                break;
            }

        }

        //它的底层也是基于jdk自省的
        user = new User();
        user.setUsername("zhangbo");
        BeanUtils.setProperty(user,"userName","张峰");
        System.out.println(user.getUsername());
    }
}
