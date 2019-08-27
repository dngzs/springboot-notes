package com.best.Jdk;

import java.util.Collections;
import java.util.List;

/**
 * @author dngzs
 * @date 2019-08-27 17:39
 */
public class EmptyListDemo {

    public static void main(String[] args) {
        List emptyList = Collections.EMPTY_LIST;
        emptyList.add("aaa");
        System.out.println(emptyList);
    }
}
