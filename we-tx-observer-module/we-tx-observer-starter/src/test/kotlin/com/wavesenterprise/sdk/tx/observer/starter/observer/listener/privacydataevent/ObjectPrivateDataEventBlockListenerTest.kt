package com.wavesenterprise.sdk.tx.observer.starter.observer.listener.privacydataevent

import com.wavesenterprise.sdk.node.domain.privacy.Data.Companion.data
import com.wavesenterprise.sdk.node.domain.privacy.PolicyItemRequest
import com.wavesenterprise.sdk.tx.observer.api.privacy.PrivateDataEvent
import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.samplePolicyDataHashTx
import com.wavesenterprise.sdk.tx.observer.starter.observer.testObjects.SimpleDataObject
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
import java.util.Base64
import java.util.Optional

@ContextConfiguration(classes = [ObjectPrivateDataEventBlockListenerTest.ListenerConfig::class])
class ObjectPrivateDataEventBlockListenerTest : AbstractPrivateEventBlockListenerTest() {

    @Autowired
    lateinit var privateDataEventListener: ObjectPrivateDataEventListener

    @Test
    fun `should handle private data event with object payload`() {
        val randomObject = SimpleDataObject("string", 10, mapOf(Pair("key", 1)))
        every {
            privacyService.data(
                request = PolicyItemRequest(
                    policyId = samplePolicyDataHashTx.policyId,
                    dataHash = samplePolicyDataHashTx.dataHash,
                )
            )
        } returns Optional.of(Base64.getEncoder().encode(objectMapper.writeValueAsBytes(randomObject)).data)

        enqueue(samplePolicyDataHashTx)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val objectValuePrivateDataEventCaptor = slot<PrivateDataEvent<SimpleDataObject>>()
        verify { privateDataEventListener.handleEvent(privateDataEvent = capture(objectValuePrivateDataEventCaptor)) }
        assertEquals(randomObject, objectValuePrivateDataEventCaptor.captured.payload)
        assertEquals(samplePolicyDataHashTx, objectValuePrivateDataEventCaptor.captured.policyDataHashTx)
    }

    interface ObjectPrivateDataEventListener {
        @TxListener
        fun handleEvent(
            privateDataEvent: PrivateDataEvent<SimpleDataObject>
        )
    }

    @TestConfiguration
    class ListenerConfig {

        @Bean
        fun listener() = mockk<ObjectPrivateDataEventListener>(relaxed = true)
    }
}
