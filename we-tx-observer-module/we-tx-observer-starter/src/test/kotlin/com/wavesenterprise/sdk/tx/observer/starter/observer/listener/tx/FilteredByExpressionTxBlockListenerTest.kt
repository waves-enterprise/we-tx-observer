package com.wavesenterprise.sdk.tx.observer.starter.observer.listener.tx

import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.createContractTxWithImageToFilter
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

@ContextConfiguration(classes = [FilteredByExpressionTxBlockListenerTest.ListenerConfig::class])
class FilteredByExpressionTxBlockListenerTest : AbstractListenerTest() {

    @Autowired
    lateinit var txListener: ExpressionTxListener

    @Test
    fun `test filter expression in block listener with matched Tx`() {
        enqueue(createContractTxWithImageToFilter)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val anyTxCaptor = slot<Tx>()
        verify { txListener.handleTx(capture(anyTxCaptor)) }

        assertEquals(createContractTxWithImageToFilter, anyTxCaptor.captured)
    }

    @Test
    fun `test filter expression in block listener without matched Tx`() {
        clearMocks(txListener)
        enqueue(mockTxList - createContractTxWithImageToFilter)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        verify(exactly = 0) { txListener.handleTx(any()) }
    }

    interface ExpressionTxListener {
        @TxListener(
            filterExpression =
            "T(com.wavesenterprise.sdk.node.domain.TxType).EXECUTED_CONTRACT.code ==" +
                " T(com.wavesenterprise.sdk.node.domain.tx.Tx).type(#this).code " +
                "&& T(com.wavesenterprise.sdk.node.domain.TxType).CREATE_CONTRACT.code ==" +
                " T(com.wavesenterprise.sdk.node.domain.tx.Tx).type(#this.tx).code " +
                "&& 'image_to_filter' == tx.image.value",
        )
        fun handleTx(tx: Tx)
    }

    @TestConfiguration
    class ListenerConfig {
        @Bean
        fun listener() = mockk<ExpressionTxListener>(relaxed = true)
    }
}
