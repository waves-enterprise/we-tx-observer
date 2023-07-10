package com.wavesenterprise.we.tx.observer.common.tx.executor

interface TxExecutor {
    fun <T> required(block: () -> T): T
    fun <T> required(timeout: Int? = null, block: () -> T): T
    fun <T> requiresNew(block: () -> T): T
    fun <T> requiresNew(timeout: Int? = null, block: () -> T): T
}
