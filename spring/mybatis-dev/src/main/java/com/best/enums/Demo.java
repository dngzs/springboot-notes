package com.best.enums;

public class Demo {

    public static void main(String[] args) {
        System.out.println(StatusEnum.DELETE.name());
        System.out.println(StatusEnum.DELETE.ordinal());
        System.out.println(StatusEnum.ENABLE.ordinal());
        System.out.println(StatusEnum.DISABLE.ordinal());
    }
}
