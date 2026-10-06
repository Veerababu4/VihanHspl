package com.vihan.hospital.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables @Scheduled jobs: token clean-up now; slot generation and no-show marking later (M4, M6). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
