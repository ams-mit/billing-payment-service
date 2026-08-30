package com.ams.billing.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "com.ams.billing.repository")
public class JpaConfig {
    // JPA auditing activated — populates @CreatedDate / @LastModifiedDate on BaseEntity
}