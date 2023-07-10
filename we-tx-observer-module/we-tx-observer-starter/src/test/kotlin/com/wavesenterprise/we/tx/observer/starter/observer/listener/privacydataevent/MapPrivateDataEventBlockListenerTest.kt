package com.wavesenterprise.we.tx.observer.starter.observer.listener.privacydataevent

import com.wavesenterprise.sdk.node.domain.privacy.Data
import com.wavesenterprise.sdk.node.domain.privacy.PolicyItemRequest
import com.wavesenterprise.we.tx.observer.api.privacy.PrivateDataEvent
import com.wavesenterprise.we.tx.observer.api.tx.TxListener
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.samplePolicyDataHashTx
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration
import org.springframework.util.Base64Utils
import java.util.Optional

@ContextConfiguration(classes = [MapPrivateDataEventBlockListenerTest.ListenerConfig::class])
class MapPrivateDataEventBlockListenerTest : AbstractPrivateEventBlockListenerTest() {

    @Autowired
    lateinit var privateDataEventListener: MapPrivateDataEventListener

    @Test
    fun `should handle private data event with map payload`() {
        val randomMap = mapOf(
            "key_list" to listOf(
                mapOf("key_int_1" to 1),
                mapOf("key_int_2" to 2)
            )
        )
        every {
            privacyService.data(
                request = PolicyItemRequest(
                    policyId = samplePolicyDataHashTx.policyId,
                    dataHash = samplePolicyDataHashTx.dataHash,
                )
            )
        } returns Optional.of(
            Data.fromByteArray(Base64Utils.encode(objectMapper.writeValueAsBytes(randomMap)))
        )
        enqueue(samplePolicyDataHashTx)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val mapValuePrivateDataEventCaptor = slot<PrivateDataEvent<Map<String, List<Map<String, Int>>>>>()
        verify { privateDataEventListener.handleEvent(privateDataEvent = capture(mapValuePrivateDataEventCaptor)) }
        assertEquals(randomMap, mapValuePrivateDataEventCaptor.captured.payload)
        assertEquals(samplePolicyDataHashTx, mapValuePrivateDataEventCaptor.captured.policyDataHashTx)
    }

    interface MapPrivateDataEventListener {
        @TxListener
        fun handleEvent(
            privateDataEvent: PrivateDataEvent<Map<String, List<Map<String, Int>>>>
        )
    }

    @TestConfiguration
    class ListenerConfig {

        @Bean
        fun listener() = mockk<MapPrivateDataEventListener>(relaxed = true)
    }
}
