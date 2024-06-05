package com.wavesenterprise.sdk.tx.observer.starter.annotation

import com.wavesenterprise.sdk.tx.observer.starter.TxObserverEnablerConfig
import org.springframework.context.annotation.Import

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Import(TxObserverEnablerConfig::class)
annotation class EnableTxObserver
