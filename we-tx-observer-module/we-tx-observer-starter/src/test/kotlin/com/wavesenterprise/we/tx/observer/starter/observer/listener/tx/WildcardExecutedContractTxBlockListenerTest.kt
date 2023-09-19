package com.wavesenterprise.we.tx.observer.starter.observer.listener.tx

import com.wavesenterprise.sdk.node.domain.tx.ExecutedContractTx
import com.wavesenterprise.we.tx.observer.api.tx.TxListener
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.callContractTxDifferentValueTypes
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.callContractTxSeveralMatchingKeys
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.callContractTxSimple
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.createContractTxSimple
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.createContractTxWithImageToFilter
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.mockTxList
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.sampleAtomicTx
import com.wavesenterprise.we.tx.observer.starter.observer.listener.AbstractListenerTest
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

@ContextConfiguration(classes = [WildcardExecutedContractTxBlockListenerTest.ListenerConfig::class])
class WildcardExecutedContractTxBlockListenerTest : AbstractListenerTest() {

    @Autowired
    lateinit var txListener: WildcardExecutedContractTxListener

    @Test
    fun `should handle ExecutedContractTx argument type with wildcard`() {
        enqueue(
            sampleAtomicTx,
            createContractTxSimple,
            createContractTxWithImageToFilter,
            callContractTxSimple,
            callContractTxDifferentValueTypes,
            callContractTxSeveralMatchingKeys
        )

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val executedContractTxCaptor = mutableListOf<ExecutedContractTx>()
        verify(exactly = 5) { txListener.handleTx(capture(executedContractTxCaptor)) }
        assertEquals(5, executedContractTxCaptor.size)
        assertThat(
            executedContractTxCaptor,
            containsInAnyOrder(
                createContractTxSimple,
                createContractTxWithImageToFilter,
                callContractTxSimple,
                callContractTxDifferentValueTypes,
                callContractTxSeveralMatchingKeys
            )
        )
    }

    @Test
    fun `should not handle other Tx types`() {
        clearMocks(txListener)
        enqueue(
            mockTxList -
                sampleAtomicTx -
                createContractTxSimple -
                createContractTxWithImageToFilter -
                callContractTxSimple -
                callContractTxDifferentValueTypes -
                callContractTxSeveralMatchingKeys
        )

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        verify(exactly = 0) { txListener.handleTx(any()) }
    }

    interface WildcardExecutedContractTxListener {
        @TxListener
        fun handleTx(executedContractTx: ExecutedContractTx)
    }

    @TestConfiguration
    class ListenerConfig {
        @Bean
        fun listener() = mockk<WildcardExecutedContractTxListener>(relaxed = true)
    }
}
