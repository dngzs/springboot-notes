package com.best.sync;

/**
 * @author dngzs
 * @date 2019-06-03 15:33
 */

import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync(proxyTargetClass = true)
public class TaskExecutorConfig {

    /**
     * 线程池名称格式
     */
    private static final String THREAD_POOL_NAME = "DownloadTaskProcessPool-%d";
    /**
     * 线程工厂名称
     */

    private static final ThreadFactory FACTORY = new BasicThreadFactory.Builder().namingPattern(THREAD_POOL_NAME).daemon(true).build();
    @Bean
    public TaskExecutor taskExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setThreadFactory(FACTORY);
        // 设置核心线程数
        executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
        // 设置最大线程数
        executor.setMaxPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        // 设置队列容量
        executor.setQueueCapacity(500);
        // 设置线程活跃时间（秒）
        executor.setKeepAliveSeconds(60);
        // 设置拒绝策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 等待所有任务结束后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        return executor;
    }
}
