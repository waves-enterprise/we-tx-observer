package com.wavesenterprise.sdk.tx.observer.starter.observer.listener.keyevent

import com.wavesenterprise.sdk.node.domain.tx.CallContractTx
import com.wavesenterprise.sdk.tx.observer.api.key.KeyEvent
import com.wavesenterprise.sdk.tx.observer.api.key.KeyFilter
import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.callContractTxDifferentValueTypes
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

@ContextConfiguration(classes = [IntValueKeyEventBlockListenerTest.ListenerConfig::class])
class IntValueKeyEventBlockListenerTest : AbstractListenerTest() {

    @Autowired
    lateinit var keyEventListener: IntKeyEventListener

    @Test
    fun `should handle key event with integer payload value`() {
        enqueue(callContractTxDifferentValueTypes)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val intKeyEventCaptor = slot<KeyEvent<Int>>()
        verify { keyEventListener.handleEvent(keyEvent = capture(intKeyEventCaptor)) }
        assertEquals(123123, intKeyEventCaptor.captured.payload)
        assertEquals(callContractTxDifferentValueTypes, intKeyEventCaptor.captured.tx)
        assertEquals(
            (callContractTxDifferentValueTypes.tx as CallContractTx).contractId.asBase58String(),
            intKeyEventCaptor.captured.contractId,
        )
        assertEquals("my_fav_key_int_", intKeyEventCaptor.captured.key)
    }

    interface IntKeyEventListener {
        @TxListener
        fun handleEvent(
            @KeyFilter(keyPrefix = "my_fav_key_int_")
            keyEvent: KeyEvent<Int>,
        )
    }

    @TestConfiguration
    class ListenerConfig {
        @Bean
        fun listener() = mockk<IntKeyEventListener>(relaxed = true)
    }
}
