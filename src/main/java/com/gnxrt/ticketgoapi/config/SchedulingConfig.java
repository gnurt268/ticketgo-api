package com.gnxrt.ticketgoapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig implements SchedulingConfigurer {

    private static final int POOL_SIZE = 5;
    private static final String THREAD_NAME_PREFIX = "ticketgo-scheduler-";

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(POOL_SIZE);
        scheduler.setThreadNamePrefix(THREAD_NAME_PREFIX);
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        scheduler.setErrorHandler(throwable -> {
            System.err.println("Scheduled task error: " + throwable.getMessage());
            throwable.printStackTrace();
        });
        scheduler.setRejectedExecutionHandler((runnable, executor) -> {
            System.err.println("Task rejected, scheduler queue is full");
        });
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler());
    }
}