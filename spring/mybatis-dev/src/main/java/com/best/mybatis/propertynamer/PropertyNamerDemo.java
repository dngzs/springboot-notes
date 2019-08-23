package com.best.mybatis.propertynamer;

import org.apache.ibatis.reflection.property.PropertyNamer;

/**
 * @author dngzs
 * @date 2019-08-16 10:12
 */
public class PropertyNamerDemo {

    public static void main(String[] args) {
        String a = "setabbb";
        System.out.println(PropertyNamer.methodToProperty(a));
    }
}
