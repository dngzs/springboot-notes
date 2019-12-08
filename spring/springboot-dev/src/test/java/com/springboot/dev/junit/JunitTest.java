package com.springboot.dev.junit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 第一个junit
 *
 * @author dngzs
 * @date 2019-12-05 15:35
 */
public class JunitTest {

    @Test
    public void test1() {
        System.out.println("test1...................");
    }

    @Test
    public void test2() {
        System.out.println("test2................");
    }


    @BeforeClass
    public static void beforeClass2() {
        try {
            Thread.sleep(10000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("beforeClass2() method executed.");
    }

    @BeforeClass
    public static void beforeClass1() {
        System.out.println("beforeClass1() method executed.");
    }



    @AfterClass
    public static void afterClass1() {
        System.out.println("afterClass1() method executed.");
    }

    @AfterClass
    public static void afterClass2() {
        System.out.println("afterClass2() method executed.");
    }


    @Before
    public void before1() {
        System.out.println("before1() method executed.");
    }
    @Before
    public void before2() {
        System.out.println("before2() method executed.");
    }

    @After
    public void after1() {
        System.out.println("after1() method executed");
    }

    @After
    public void after2() {
        System.out.println("after2() method executed");
    }

    public void after3() {
        System.out.println("after3() method executed");
    }
}
