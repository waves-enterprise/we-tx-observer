package com.wavesenterprise.sdk.tx.observer.common.tx.executor

import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionException
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

open class TxExecutorImpl(
    private val transactionTemplate: TransactionTemplate,
) : TxExecutor {

    @Transactional(propagation = Propagation.REQUIRED)
    override fun <T> required(block: () -> T): T =
        block()

    override fun <T> required(timeout: Int?, block: () -> T): T =
        execute(
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRED,
            timeout = timeout,
            block = block,
        )

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    override fun <T> requiresNew(block: () -> T): T =
        block()

    override fun <T> requiresNew(timeout: Int?, block: () -> T): T =
        execute(
            propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW,
            timeout = timeout,
            block = block,
        )

    private fun <T> execute(
        propagationBehavior: Int? = null,
        isolationLevel: Int? = null,
        timeout: Int? = null,
        block: () -> T,
    ): T = with(transactionTemplate) {
        if (propagationBehavior != null)
            this.propagationBehavior = propagationBehavior
        if (isolationLevel != null)
            this.isolationLevel = isolationLevel
        if (timeout != null)
            this.timeout = timeout
        val executionResult = execute {
            ExecutionResult(block())
        }
        return if (executionResult != null) executionResult.value
        else throw NoResultTransactionException("No result for execute")
    }

    companion object {
        private data class ExecutionResult<T>(val value: T)

        class NoResultTransactionException(
            override val message: String,
        ) : TransactionException(message)
    }
}
