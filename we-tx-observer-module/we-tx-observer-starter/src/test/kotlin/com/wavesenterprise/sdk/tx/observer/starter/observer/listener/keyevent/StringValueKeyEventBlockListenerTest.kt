package com.wavesenterprise.sdk.tx.observer.starter.observer.listener.keyevent

import com.wavesenterprise.sdk.node.domain.tx.CallContractTx
import com.wavesenterprise.sdk.tx.observer.api.key.KeyEvent
import com.wavesenterprise.sdk.tx.observer.api.key.KeyFilter
import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.callContractTxSimple
import com.wavesenterprise.sdk.tx.observer.starter.observer.listener.AbstractListenerTest
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [StringValueKeyEventBlockListenerTest.ListenerConfig::class])
class StringValueKeyEventBlockListenerTest : AbstractListenerTest() {

    @Autowired
    lateinit var keyEventListener: StringKeyEventListener

    @Test
    fun `should handle key event with string payload value`() {
        enqueue(callContractTxSimple)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val stringKeyEventCaptor = slot<KeyEvent<String>>()
        verify { keyEventListener.handleEvent(keyEvent = capture(stringKeyEventCaptor)) }
        assertEquals("my fav object new value", stringKeyEventCaptor.captured.payload)
        assertEquals(callContractTxSimple, stringKeyEventCaptor.captured.tx)
        assertEquals(
            (callContractTxSimple.tx as CallContractTx).contractId.asBase58String(),
            stringKeyEventCaptor.captured.contractId
        )
        assertEquals("my_fav_object_string_", stringKeyEventCaptor.captured.key)
    }

    interface StringKeyEventListener {
        @TxListener
        fun handleEvent(
            @KeyFilter(keyPrefix = "my_fav_object_string_")
            keyEvent: KeyEvent<String>
        )
    }

    @TestConfiguration
    class ListenerConfig {
        @Bean
        fun listener() = mockk<StringKeyEventListener>(relaxed = true)
    }
}
