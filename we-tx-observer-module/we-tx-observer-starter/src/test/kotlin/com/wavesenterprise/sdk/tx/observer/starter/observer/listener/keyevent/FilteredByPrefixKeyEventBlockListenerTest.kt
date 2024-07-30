package com.wavesenterprise.sdk.tx.observer.starter.observer.listener.keyevent

import com.wavesenterprise.sdk.node.domain.DataValue
import com.wavesenterprise.sdk.node.domain.tx.CallContractTx
import com.wavesenterprise.sdk.tx.observer.api.key.KeyEvent
import com.wavesenterprise.sdk.tx.observer.api.key.KeyFilter
import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.callContractTxSeveralMatchingKeys
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.mockTxList
import com.wavesenterprise.sdk.tx.observer.starter.observer.listener.AbstractListenerTest
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [FilteredByPrefixKeyEventBlockListenerTest.ListenerConfig::class])
class FilteredByPrefixKeyEventBlockListenerTest : AbstractListenerTest() {

    @Autowired
    lateinit var keyEventListener: StringKeyEventListener

    @Test
    fun `should handle key event filtered by key prefix`() {
        enqueue(callContractTxSeveralMatchingKeys)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val stringKeyEventCaptor = mutableListOf<KeyEvent<String>>()
        verify(exactly = 3) {
            keyEventListener.handleEvent(keyEvent = capture(stringKeyEventCaptor))
        }
        stringKeyEventCaptor.forEachIndexed { idx, keyEvent ->
            assertEquals(
                (callContractTxSeveralMatchingKeys.results[idx].value as DataValue.StringDataValue).value,
                keyEvent.payload,
            )
            assertEquals(callContractTxSeveralMatchingKeys, keyEvent.tx)
            assertEquals(callContractTxSeveralMatchingKeys.results[idx].key.value, keyEvent.key)
            assertEquals(
                (callContractTxSeveralMatchingKeys.tx as CallContractTx).contractId.asBase58String(),
                keyEvent.contractId,
            )
        }
    }

    @Test
    fun `should not handle other key events`() {
        clearMocks(keyEventListener)
        enqueue(mockTxList - callContractTxSeveralMatchingKeys)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        verify(exactly = 0) { keyEventListener.handleEvent(any()) }
    }

    interface StringKeyEventListener {
        @TxListener
        fun handleEvent(
            @KeyFilter(keyPrefix = "my_fav_multi_key_string_")
            keyEvent: KeyEvent<String>,
        )
    }

    @TestConfiguration
    class ListenerConfig {
        @Bean
        fun listener() = mockk<StringKeyEventListener>(relaxed = true)
    }
}
