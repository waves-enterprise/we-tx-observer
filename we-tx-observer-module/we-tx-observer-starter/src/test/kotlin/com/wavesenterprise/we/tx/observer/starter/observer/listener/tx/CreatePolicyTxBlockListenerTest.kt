package com.wavesenterprise.we.tx.observer.starter.observer.listener.tx

import com.wavesenterprise.sdk.node.domain.tx.CreatePolicyTx
import com.wavesenterprise.we.tx.observer.api.tx.TxListener
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.mockTxList
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.sampleAtomicTx
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.sampleCreatePolicyTx
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

@ContextConfiguration(classes = [CreatePolicyTxBlockListenerTest.ListenerConfig::class])
class CreatePolicyTxBlockListenerTest : AbstractListenerTest() {

    @Autowired
    lateinit var txListener: CreatePolicyTxListener

    @Test
    fun `should handle CreatePolicyTx`() {
        enqueue(
            sampleAtomicTx,
            sampleCreatePolicyTx
        )

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val createPolicyTxCaptor = slot<CreatePolicyTx>()
        verify(exactly = 1) { txListener.handleTx(createPolicyTx = capture(createPolicyTxCaptor)) }
        assertEquals(createPolicyTxCaptor.captured, sampleCreatePolicyTx)
    }

    @Test
    fun `should not handle other Tx types`() {
        clearMocks(txListener)
        enqueue(mockTxList - sampleAtomicTx - sampleCreatePolicyTx)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        verify(exactly = 0) { txListener.handleTx(any()) }
    }

    interface CreatePolicyTxListener {
        @TxListener
        fun handleTx(createPolicyTx: CreatePolicyTx)
    }

    @TestConfiguration
    class ListenerConfig {
        @Bean
        fun listener() = mockk<CreatePolicyTxListener>(relaxed = true)
    }
}
