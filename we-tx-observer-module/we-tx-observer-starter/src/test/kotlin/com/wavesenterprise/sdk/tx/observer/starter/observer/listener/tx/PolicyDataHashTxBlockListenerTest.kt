package com.wavesenterprise.sdk.tx.observer.starter.observer.listener.tx

import com.wavesenterprise.sdk.node.domain.tx.PolicyDataHashTx
import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.mockTxList
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.sampleAtomicTx
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.samplePolicyDataHashTx
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

@ContextConfiguration(classes = [PolicyDataHashTxBlockListenerTest.ListenerConfig::class])
class PolicyDataHashTxBlockListenerTest : AbstractListenerTest() {

    @Autowired
    lateinit var txListener: PolicyDataHashTxListener

    @Test
    fun `should handle PolicyDataHashTx`() {
        enqueue(
            sampleAtomicTx,
            samplePolicyDataHashTx,
        )

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val policyDataHashTxCaptor = slot<PolicyDataHashTx>()
        verify { txListener.handleTx(policyDataHashTx = capture(policyDataHashTxCaptor)) }
        assertEquals(policyDataHashTxCaptor.captured, samplePolicyDataHashTx)
    }

    @Test
    fun `should not handle other Tx types`() {
        clearMocks(txListener)
        enqueue(mockTxList - sampleAtomicTx - samplePolicyDataHashTx)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        verify(exactly = 0) { txListener.handleTx(any()) }
    }

    interface PolicyDataHashTxListener {
        @TxListener
        fun handleTx(policyDataHashTx: PolicyDataHashTx)
    }

    @TestConfiguration
    class ListenerConfig {
        @Bean
        fun listener() = mockk<PolicyDataHashTxListener>(relaxed = true)
    }
}
