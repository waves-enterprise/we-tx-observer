package com.wavesenterprise.sdk.tx.observer.starter.observer.listener.tx

import com.wavesenterprise.sdk.node.domain.tx.ExecutedContractTx
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.createContractTxSimple
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.createContractTxWithImageToFilter
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.sampleAtomicTx
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

@ContextConfiguration(classes = [ExecutedCreateContractTxBlockListenerTest.ListenerConfig::class])
class ExecutedCreateContractTxBlockListenerTest : AbstractListenerTest() {

    @Autowired
    lateinit var txListener: ExecutedCreateContractTxListener

    @Test
    fun `should handle ExecutedContractTx for CreateContractTx`() {
        clearMocks(txListener)
        enqueue(
            sampleAtomicTx,
            createContractTxSimple,
            createContractTxWithImageToFilter,
        )

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val createExecutedContractTxCaptor = mutableListOf<ExecutedContractTx>()
        verify(exactly = 2) { txListener.handleTx(executedContractTx = capture(createExecutedContractTxCaptor)) }
        assertEquals(2, createExecutedContractTxCaptor.size)
        assertThat(
            createExecutedContractTxCaptor,
            containsInAnyOrder(
                createContractTxSimple,
                createContractTxWithImageToFilter,
            ),
        )
    }

    @Test
    fun `should not handle other Tx types`() {
        enqueue(
            TestDataFactory.createContractTx(),
            TestDataFactory.callContractTx(),
            TestDataFactory.genesisTx(),
            TestDataFactory.permitTx(),
        )

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        verify(exactly = 0) { txListener.handleTx(any()) }
    }

    interface ExecutedCreateContractTxListener {
        @TxListener
        fun handleTx(executedContractTx: ExecutedContractTx)
    }

    @TestConfiguration
    class ListenerConfig {
        @Bean
        fun listener() = mockk<ExecutedCreateContractTxListener>(relaxed = true)
    }
}
