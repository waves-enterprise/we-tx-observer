package com.wavesenterprise.sdk.tx.observer.starter.observer.util

import com.wavesenterprise.sdk.tx.observer.common.tx.executor.TxExecutor

object TxExecutorStub : TxExecutor {
    override fun <T> required(block: () -> T): T =
        block()

    override fun <T> required(timeout: Int?, block: () -> T): T =
        block()

    override fun <T> requiresNew(block: () -> T): T =
        block()

    override fun <T> requiresNew(timeout: Int?, block: () -> T): T =
        block()
}
