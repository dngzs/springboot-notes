package com.springboot.dev.tools;

import org.springframework.util.StopWatch;

public class StopWatchDemo {

    public static void main(String[] args) throws InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("任务一");
        Thread.sleep(1000L);
        stopWatch.stop();

        stopWatch.start("任务二");
        Thread.sleep(1000L);
        stopWatch.stop();

        System.out.println(stopWatch.prettyPrint());
    }
}
