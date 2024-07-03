package com.wavesenterprise.sdk.tx.tracker.starter

import com.wavesenterprise.sdk.tx.tracker.core.spring.component.ScheduledTxTracker
import com.wavesenterprise.sdk.tx.tracker.starter.properties.TxTrackerProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.support.PeriodicTrigger
import org.springframework.scheduling.support.ScheduledMethodRunnable
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod

@Configuration
@EnableScheduling
class TxTrackerSchedulerConfig {

    @Autowired
    lateinit var scheduledTxTracker: ScheduledTxTracker

    @Bean
    fun txTrackerScheduler(
        txTrackerProperties: TxTrackerProperties,
    ) =
        ThreadPoolTaskScheduler().apply {
            scheduledTxTracker.let { scheduledTxTracker ->
                if (txTrackerProperties.enabled) {
                    poolSize = 10
                    setThreadNamePrefix("tx-observer-pool-")
                    initialize()
                    schedule(
                        scheduledMethodRunnable(
                            scheduledTxTracker,
                            ScheduledTxTracker::trackPendingTx,
                        ),
                        PeriodicTrigger(txTrackerProperties.fixedDelay),
                    )
                }
            }
        }

    companion object {
        private inline fun <reified T : Any> scheduledMethodRunnable(
            scheduled: T,
            kFunction1: KFunction<T>,
        ): ScheduledMethodRunnable =
            ScheduledMethodRunnable(
                scheduled,
                kFunction1.javaMethod ?: error("Cannot get method for ${T::class.simpleName}"),
            )
    }
}
