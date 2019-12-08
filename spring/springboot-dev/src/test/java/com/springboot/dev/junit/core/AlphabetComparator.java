package com.springboot.dev.junit.core;

import org.junit.runner.Description;

import java.util.Comparator;

/**
 * @author dngzs
 * @date 2019-12-05 15:46
 */
public class AlphabetComparator implements Comparator<Description> {
    @Override
    public int compare(Description desc1, Description desc2) {
        return desc1.getMethodName().compareTo(desc2.getMethodName());
    }
}