package com.driver.bookMyShow.modules.receipt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration for Receipt Generation
 * 
 * Enables asynchronous receipt generation without blocking payment completion
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "receiptGenerationExecutor")
    public Executor receiptGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("receipt-gen-");
        executor.initialize();
        return executor;
    }
}
