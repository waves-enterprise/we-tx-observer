package com.wavesenterprise.we.tx.observer.api.tx

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TxListener(
    val filterExpression: String = "",
)
