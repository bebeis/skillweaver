package com.bebeis.skillweaver.api.common.config

import com.bebeis.skillweaver.api.common.security.JwtProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class AppConfig
