package com.campustrade.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AiAsyncConfig {

    @Value("${ai.async.core-pool-size:2}")
    private int corePoolSize;

    @Value("${ai.async.max-pool-size:4}")
    private int maxPoolSize;

    @Value("${ai.async.queue-capacity:100}")
    private int queueCapacity;

    @Value("${ai.async.await-termination-seconds:10}")
    private int awaitTerminationSeconds;

    @Bean("aiTaskExecutor")
    public ThreadPoolTaskExecutor aiTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("ai-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();
        return executor;
    }
}
