package com.wavesenterprise.sdk.tx.observer.starter.observer.listener.tx

import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.mockTxList
import com.wavesenterprise.sdk.tx.observer.starter.observer.listener.AbstractListenerTest
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [AllTxBlockListenerTest.ListenerConfig::class])
class AllTxBlockListenerTest : AbstractListenerTest() {

    @Autowired
    lateinit var txsListener: TxsListener

    @Test
    fun `should handle all Txs with no filter`() {
        enqueue(mockTxList)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val anyTxCaptor = mutableListOf<Tx>()
        val numInvocations = mockTxList.size
        verify(exactly = numInvocations) { txsListener.handleTx(tx = capture(anyTxCaptor)) }
        assertThat(
            anyTxCaptor,
            containsInAnyOrder(
                *(mockTxList).toTypedArray()
            )
        )
    }

    interface TxsListener {
        @TxListener
        fun handleTx(tx: Tx)
    }

    @TestConfiguration
    class ListenerConfig {
        @Bean
        fun listener() = mockk<TxsListener>(relaxed = true)
    }
}
