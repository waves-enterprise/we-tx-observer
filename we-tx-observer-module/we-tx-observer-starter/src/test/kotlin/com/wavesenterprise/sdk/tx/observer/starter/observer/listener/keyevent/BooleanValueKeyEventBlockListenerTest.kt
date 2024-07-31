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

@ContextConfiguration(classes = [BooleanValueKeyEventBlockListenerTest.ListenerConfig::class])
class BooleanValueKeyEventBlockListenerTest : AbstractListenerTest() {

    @Autowired
    lateinit var keyEventListener: BooleanKeyEventListener

    @Test
    fun `should handle key event with boolean payload value`() {
        enqueue(callContractTxDifferentValueTypes)
        val booleanKeyEventCaptor = slot<KeyEvent<Boolean>>()

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        verify { keyEventListener.handleEvent(keyEvent = capture(booleanKeyEventCaptor)) }
        assertEquals(true, booleanKeyEventCaptor.captured.payload)
        assertEquals(callContractTxDifferentValueTypes, booleanKeyEventCaptor.captured.tx)
        assertEquals(
            (callContractTxDifferentValueTypes.tx as CallContractTx).contractId.asBase58String(),
            booleanKeyEventCaptor.captured.contractId,
        )
        assertEquals("my_fav_object_bool_", booleanKeyEventCaptor.captured.key)
    }

    interface BooleanKeyEventListener {
        @TxListener
        fun handleEvent(
            @KeyFilter(keyPrefix = "my_fav_object_bool_")
            keyEvent: KeyEvent<Boolean>,
        )
    }

    @TestConfiguration
    class ListenerConfig {
        @Bean
        fun listener() = mockk<BooleanKeyEventListener>(relaxed = true)
    }
}
