package com.wavesenterprise.we.tx.observer.starter.cache

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.bind.DefaultValue
import org.springframework.boot.convert.DurationUnit
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration
import java.time.temporal.ChronoUnit

@Configuration
@EnableCaching
@EnableConfigurationProperties(DefaultTimeBasedCacheProperties::class)
class DefaultTimeBasedCacheConfiguration(val defaultTimeBasedCacheProperties: DefaultTimeBasedCacheProperties) {
    @Bean
    fun defaultTimeBasedCacheManager(): CacheManager =
        CaffeineCacheManager().apply {
            setCaffeine(
                Caffeine.newBuilder()
                    .expireAfterWrite(defaultTimeBasedCacheProperties.expireAfterWrite)
            )
        }
}

@ConfigurationProperties("cache.default-time-based")
@ConstructorBinding
data class DefaultTimeBasedCacheProperties(
    @DurationUnit(ChronoUnit.SECONDS)
    @DefaultValue("60s")
    var expireAfterWrite: Duration
)
