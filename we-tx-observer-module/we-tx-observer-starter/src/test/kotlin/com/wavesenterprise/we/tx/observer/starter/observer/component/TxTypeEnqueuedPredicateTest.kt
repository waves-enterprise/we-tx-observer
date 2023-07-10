package com.wavesenterprise.we.tx.observer.starter.observer.component

import com.wavesenterprise.sdk.node.domain.TxType
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.we.tx.observer.core.spring.component.TxTypeEnqueuedPredicate
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TxTypeEnqueuedPredicateTest {

    @Test
    fun `predicate true with empty tx types`() {
        val predicate = TxTypeEnqueuedPredicate(
            txTypes = listOf(),
        )

        assertTrue(
            predicate.isEnqueued(
                tx = TestDataFactory.createContractTx(),
            )
        )
    }

    @Test
    fun `predicate true with correct tx type`() {
        val predicate = TxTypeEnqueuedPredicate(
            txTypes = listOf(TxType.CREATE_CONTRACT),
        )

        assertTrue(
            predicate.isEnqueued(
                tx = TestDataFactory.createContractTx(),
            )
        )
    }

    @Test
    fun `predicate false with incorrect tx type`() {
        val predicate = TxTypeEnqueuedPredicate(
            txTypes = listOf(TxType.GENESIS),
        )

        assertFalse(
            predicate.isEnqueued(
                tx = TestDataFactory.createContractTx(),
            )
        )
    }
}
