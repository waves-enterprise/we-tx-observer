package com.wavesenterprise.sdk.tx.observer.starter.observer.listener.keyevent

import com.wavesenterprise.sdk.node.domain.tx.CallContractTx
import com.wavesenterprise.sdk.tx.observer.api.key.KeyEvent
import com.wavesenterprise.sdk.tx.observer.api.key.KeyFilter
import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.callContractTxDifferentValueTypes
import com.wavesenterprise.sdk.tx.observer.starter.observer.listener.AbstractListenerTest
import com.wavesenterprise.sdk.tx.observer.starter.observer.testObjects.SimpleDataObject
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [ObjectValueKeyEventBlockListenerTest.ListenerConfig::class])
class ObjectValueKeyEventBlockListenerTest : AbstractListenerTest() {

    @Autowired
    lateinit var keyEventListener: ObjectKeyEventListener

    @Test
    fun `should handle key event with object payload value`() {
        enqueue(callContractTxDifferentValueTypes)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val objectKeyEventCaptor = slot<KeyEvent<SimpleDataObject>>()
        verify { keyEventListener.handleEvent(keyEvent = capture(objectKeyEventCaptor)) }
        assertEquals(
            SimpleDataObject("string", 10, mapOf(Pair("key", 1))),
            objectKeyEventCaptor.captured.payload,
        )
        assertEquals(callContractTxDifferentValueTypes, objectKeyEventCaptor.captured.tx)
        assertEquals(
            (callContractTxDifferentValueTypes.tx as CallContractTx).contractId.asBase58String(),
            objectKeyEventCaptor.captured.contractId,
        )
        assertEquals("my_fav_object_object_", objectKeyEventCaptor.captured.key)
    }

    interface ObjectKeyEventListener {
        @TxListener
        fun handleEvent(
            @KeyFilter(keyPrefix = "my_fav_object_object_")
            keyEvent: KeyEvent<SimpleDataObject>,
        )
    }

    @TestConfiguration
    class ListenerConfig {
        @Bean
        fun listener() = mockk<ObjectKeyEventListener>(relaxed = true)
    }
}
