package com.best.mybatis.reflector;

import org.apache.ibatis.reflection.ReflectionException;

import java.lang.reflect.Method;

/**
 * @author dngzs
 * @date 2019-08-16 11:23
 */
public class SettingDemo {

    public void getB(Parent parent){
    }

    public void getA(Child child){

    }

    public void getC(Child ...child){

    }

    public static void main(String[] args) throws NoSuchMethodException {
        Method getB = SettingDemo.class.getMethod("getB",Parent.class);
        Method getA = SettingDemo.class.getMethod("getA",Child.class);
        Method[] methods = SettingDemo.class.getMethods();
        for (Method method : methods) {
            Class<?>[] parameterTypes = method.getParameterTypes();
            System.out.println(parameterTypes);
        }

        Method method = pickBetterSetter(getB, getA, null);
        System.out.println(method.getName());
        System.out.println(SettingDemo.class.isInterface());
    }


    private static  Method pickBetterSetter(Method setter1, Method setter2, String property) {
        if (setter1 == null) {
            return setter2;
        }
        Class<?> paramType1 = setter1.getParameterTypes()[0];
        Class<?> paramType2 = setter2.getParameterTypes()[0];
        if (paramType1.isAssignableFrom(paramType2)) {
            return setter2;
        } else if (paramType2.isAssignableFrom(paramType1)) {
            return setter1;
        }
        throw new ReflectionException("Ambiguous setters defined for property '" + property + "' in class '"
                + setter2.getDeclaringClass() + "' with types '" + paramType1.getName() + "' and '"
                + paramType2.getName() + "'.");
    }

}
