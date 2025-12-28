package com.fintech.walletservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduled task execution capability.
 * Required for @Scheduled annotations to work.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
