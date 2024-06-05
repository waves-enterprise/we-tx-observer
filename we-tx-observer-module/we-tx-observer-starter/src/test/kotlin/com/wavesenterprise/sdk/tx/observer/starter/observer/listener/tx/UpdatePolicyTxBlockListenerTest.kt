package com.wavesenterprise.sdk.tx.observer.starter.observer.listener.tx

import com.wavesenterprise.sdk.node.domain.tx.UpdatePolicyTx
import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.mockTxList
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.sampleUpdatePolicyTx
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

@ContextConfiguration(classes = [UpdatePolicyTxBlockListenerTest.ListenerConfig::class])
class UpdatePolicyTxBlockListenerTest : AbstractListenerTest() {

    @Autowired
    lateinit var txListener: UpdatePolicyTxListener

    @Test
    fun `should handle UpdatePolicyTx`() {
        enqueue(sampleUpdatePolicyTx)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val updatePolicyTxCaptor = slot<UpdatePolicyTx>()
        verify { txListener.handleTx(updatePolicyTx = capture(updatePolicyTxCaptor)) }
        assertEquals(sampleUpdatePolicyTx, updatePolicyTxCaptor.captured)
    }

    @Test
    fun `should not handle other Tx types`() {
        clearMocks(txListener)
        enqueue(mockTxList - sampleUpdatePolicyTx)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        verify(exactly = 0) { txListener.handleTx(any()) }
    }

    interface UpdatePolicyTxListener {
        @TxListener
        fun handleTx(updatePolicyTx: UpdatePolicyTx)
    }

    @TestConfiguration
    class ListenerConfig {
        @Bean
        fun listener() = mockk<UpdatePolicyTxListener>(relaxed = true)
    }
}
