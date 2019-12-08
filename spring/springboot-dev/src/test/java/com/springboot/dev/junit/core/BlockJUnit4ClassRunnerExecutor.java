package com.springboot.dev.junit.core;

import com.springboot.dev.junit.JunitTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.internal.runners.ErrorReportingRunner;
import org.junit.runner.Result;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.manipulation.Sorter;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;

/**
 * @author dngzs
 * @date 2019-12-05 15:43
 */
@Slf4j
public class BlockJUnit4ClassRunnerExecutor {

    public static void main(String[] args) {
        RunNotifier notifier = new RunNotifier();
        Result result = new Result();
        notifier.addFirstListener(result.createListener());
        notifier.addListener(new LogRunListener());

        Runner runner = null;
        try {
            runner = new BlockJUnit4ClassRunner(JunitTest.class);
            try {
                ((BlockJUnit4ClassRunner) runner).filter(new MethodNameFilter("testFilteredOut"));
            } catch (NoTestsRemainException e) {
                System.out.println("All methods are been filtered out");
                return;
            }
            ((BlockJUnit4ClassRunner) runner).sort(new Sorter(new AlphabetComparator()));
        } catch (Throwable e) {
            runner = new ErrorReportingRunner(JunitTest.class, e);
        }
        notifier.fireTestRunStarted(runner.getDescription());
        runner.run(notifier);
        notifier.fireTestRunFinished(result);
    }

}
