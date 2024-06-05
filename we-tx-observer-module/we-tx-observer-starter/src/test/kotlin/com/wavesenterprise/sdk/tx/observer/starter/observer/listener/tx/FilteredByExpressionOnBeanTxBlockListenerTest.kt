package com.wavesenterprise.sdk.tx.observer.starter.observer.listener.tx

import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.callContractTxDifferentValueTypes
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.mockTxList
import com.wavesenterprise.sdk.tx.observer.starter.observer.listener.AbstractListenerTest
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [FilteredByExpressionOnBeanTxBlockListenerTest.ListenerConfig::class])
class FilteredByExpressionOnBeanTxBlockListenerTest : AbstractListenerTest() {

    @Autowired
    lateinit var txListener: BeanResolverExpressionTxListener

    @Test
    fun `should handle Tx matched by filter with expression on bean`() {
        enqueue(callContractTxDifferentValueTypes)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val anyTxCaptor = slot<Tx>()
        verify { txListener.handleTx(tx = capture(anyTxCaptor)) }
        assertEquals(callContractTxDifferentValueTypes, anyTxCaptor.captured)
    }

    @Test
    fun `should not handle other Txs`() {
        clearMocks(txListener)
        enqueue(mockTxList - callContractTxDifferentValueTypes)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        verify(exactly = 0) { txListener.handleTx(any()) }
    }

    interface BeanResolverExpressionTxListener {
        @TxListener(filterExpression = "@customConditionBean.checkTx(#root)")
        fun handleTx(tx: Tx)
    }

    class TxChecker {
        @Suppress("unused")
        fun checkTx(tx: Tx): Boolean =
            tx.id == TxId.fromByteArray("update_tx_for_my_fav_int_and_bool_and_objects".toByteArray())
    }

    @TestConfiguration
    class ListenerConfig {
        @Bean
        fun listener() = mockk<BeanResolverExpressionTxListener>(relaxed = true)

        @Bean
        fun customConditionBean() = TxChecker()
    }
}
