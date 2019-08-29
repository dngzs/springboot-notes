package com.best.jdk;

import java.math.BigDecimal;

public class NumberDemo {
    public static void main(String[] args) {
        BigDecimal v = new BigDecimal("10");
        BigDecimal base = new BigDecimal("25");
        System.out.println(v.compareTo(base)<=0);
    }
}
