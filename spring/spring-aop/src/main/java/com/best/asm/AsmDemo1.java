package com.best.asm;

import aj.org.objectweb.asm.ClassReader;
import aj.org.objectweb.asm.ClassWriter;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * @author dngzs
 * @date 2019-08-21 14:42
 */
public class AsmDemo1 {

    public static void main(String[] args) throws Exception{
        InputStream inputStream = new FileInputStream("C:\\Users\\bg317957\\Desktop\\springboot-notes\\spring\\spring-aop\\target\\classes\\com\\best\\asm\\Bc.class");
        ClassReader classReader = new ClassReader(inputStream);
    }
}
