package com.wavesenterprise.we.tx.observer.starter.observer.listener.tx

import com.wavesenterprise.sdk.node.domain.tx.AtomicTx
import com.wavesenterprise.we.tx.observer.api.tx.TxListener
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.mockTxList
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.sampleAtomicTx
import com.wavesenterprise.we.tx.observer.starter.observer.listener.AbstractListenerTest
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

@ContextConfiguration(classes = [AtomicTxBlockListenerTest.ListenerConfig::class])
class AtomicTxBlockListenerTest : AbstractListenerTest() {

    @Autowired
    lateinit var txListener: AtomicTxListener

    @Test
    fun `should handle AtomicTx`() {
        enqueue(sampleAtomicTx)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val atomicTxCaptor = slot<AtomicTx>()
        verify {
            txListener.handleTx(atomicTx = capture(atomicTxCaptor))
        }
        assertEquals(sampleAtomicTx, atomicTxCaptor.captured)
    }

    @Test
    fun `should not handle other Tx types`() {
        clearMocks(txListener)
        enqueue(mockTxList - sampleAtomicTx)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        verify(exactly = 0) { txListener.handleTx(any()) }
    }

    interface AtomicTxListener {
        @TxListener
        fun handleTx(atomicTx: AtomicTx)
    }

    @TestConfiguration
    class ListenerConfig {
        @Bean
        fun listener() = mockk<AtomicTxListener>(relaxed = true)
    }
}
