package com.springboot.dev.junit.core;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import java.util.HashSet;
import java.util.Set;

/**
 * @author dngzs
 * @date 2019-12-05 15:46
 */
public class MethodNameFilter extends Filter {
    private final Set<String> excludedMethods = new HashSet<String>();

    public MethodNameFilter(String... excludedMethods) {
        for (String method : excludedMethods) {
            this.excludedMethods.add(method);
        }
    }

    @Override
    public boolean shouldRun(Description description) {
        String methodName = description.getMethodName();
        if (excludedMethods.contains(methodName)) {
            return false;
        }
        return true;
    }

    @Override
    public String describe() {
        return this.getClass().getSimpleName() + "-excluded methods: " +
                excludedMethods;
    }
}

