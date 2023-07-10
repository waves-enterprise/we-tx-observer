package com.wavesenterprise.we.tx.observer.starter.observer.listener.keyevent

import com.wavesenterprise.sdk.node.domain.tx.CallContractTx
import com.wavesenterprise.we.tx.observer.api.key.KeyEvent
import com.wavesenterprise.we.tx.observer.api.key.KeyFilter
import com.wavesenterprise.we.tx.observer.api.tx.TxListener
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.callContractTxDifferentValueTypes
import com.wavesenterprise.we.tx.observer.starter.observer.listener.AbstractListenerTest
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [MapValueKeyEventBlockListenerTest.ListenerConfig::class])
class MapValueKeyEventBlockListenerTest : AbstractListenerTest() {

    @Autowired
    lateinit var keyEventListener: MapKeyEventListener

    @Test
    fun `should handle key event with map payload value`() {
        enqueue(callContractTxDifferentValueTypes)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val mapKeyEventCaptor = slot<KeyEvent<Map<String, List<Map<String, Int>>>>>()
        verify { keyEventListener.handleEvent(keyEvent = capture(mapKeyEventCaptor)) }
        assertEquals(
            mapOf(
                "key_list" to listOf(
                    mapOf("key_int_1" to 1),
                    mapOf("key_int_2" to 2)
                )
            ),
            mapKeyEventCaptor.captured.payload
        )
        assertEquals(callContractTxDifferentValueTypes, mapKeyEventCaptor.captured.tx)
        assertEquals(
            (callContractTxDifferentValueTypes.tx as CallContractTx).contractId.asBase58String(),
            mapKeyEventCaptor.captured.contractId
        )
        assertEquals("my_fav_object_map_", mapKeyEventCaptor.captured.key)
    }

    interface MapKeyEventListener {
        @TxListener
        fun handleEvent(
            @KeyFilter(keyPrefix = "my_fav_object_map_")
            keyEvent: KeyEvent<Map<String, List<Map<String, Int>>>>
        )
    }

    @TestConfiguration
    class ListenerConfig {
        @Bean
        fun listener() = mockk<MapKeyEventListener>(relaxed = true)
    }
}
