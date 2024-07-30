package com.wavesenterprise.sdk.tx.observer.starter.observer.listener.tx

import com.wavesenterprise.sdk.node.domain.tx.ExecutedContractTx
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.callContractTxDifferentValueTypes
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.callContractTxSeveralMatchingKeys
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.callContractTxSimple
import com.wavesenterprise.sdk.tx.observer.starter.observer.listener.AbstractListenerTest
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [ExecutedCallContractTxBlockListenerTest.ListenerConfig::class])
class ExecutedCallContractTxBlockListenerTest : AbstractListenerTest() {

    @Autowired
    lateinit var txListener: ExecutedCallContractTxListener

    @Test
    fun `should handle ExecutedContractTx for CallContractTx`() {
        clearMocks(txListener)
        enqueue(
            callContractTxSimple,
            callContractTxDifferentValueTypes,
            callContractTxSeveralMatchingKeys,
        )

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val callExecutedContractTxCaptor = mutableListOf<ExecutedContractTx>()

        verify { txListener.handleTx(executedContractTx = capture(callExecutedContractTxCaptor)) }
        assertEquals(3, callExecutedContractTxCaptor.size)
        assertThat(
            callExecutedContractTxCaptor,
            containsInAnyOrder(
                callContractTxSimple,
                callContractTxDifferentValueTypes,
                callContractTxSeveralMatchingKeys,
            ),
        )
    }

    @Test
    fun `should not handle other Tx types`() {
        clearMocks(txListener)
        enqueue(
            TestDataFactory.createContractTx(),
            TestDataFactory.callContractTx(),
            TestDataFactory.genesisTx(),
            TestDataFactory.permitTx(),
        )

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)
        verify(exactly = 0) { txListener.handleTx(any()) }
    }

    interface ExecutedCallContractTxListener {
        @TxListener
        fun handleTx(executedContractTx: ExecutedContractTx)
    }

    @TestConfiguration
    class ListenerConfig {
        @Bean
        fun listener() = mockk<ExecutedCallContractTxListener>(relaxed = true)
    }
}
