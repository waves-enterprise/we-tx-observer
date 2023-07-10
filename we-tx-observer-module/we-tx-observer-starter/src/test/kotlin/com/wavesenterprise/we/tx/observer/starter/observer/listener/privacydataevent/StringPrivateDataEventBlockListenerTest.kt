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

@ContextConfiguration(classes = [StringPrivateDataEventBlockListenerTest.ListenerConfig::class])
class StringPrivateDataEventBlockListenerTest : AbstractPrivateEventBlockListenerTest() {

    @Autowired
    lateinit var privateDataEventListener: StringPrivateDataEventListener

    @Test
    fun `should handle private data event with string payload`() {
        val randomString = "Random String"
        every {
            privacyService.data(
                request = PolicyItemRequest(
                    policyId = samplePolicyDataHashTx.policyId,
                    dataHash = samplePolicyDataHashTx.dataHash,
                )
            )
        } returns Optional.of(Data.fromByteArray(Base64Utils.encode(randomString.toByteArray())))

        enqueue(samplePolicyDataHashTx)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val stringValuePrivateDataEventCaptor = slot<PrivateDataEvent<String>>()
        verify { privateDataEventListener.handleEvent(privateDataEvent = capture(stringValuePrivateDataEventCaptor)) }
        assertEquals(randomString, stringValuePrivateDataEventCaptor.captured.payload)
        assertEquals(samplePolicyDataHashTx, stringValuePrivateDataEventCaptor.captured.policyDataHashTx)
    }

    interface StringPrivateDataEventListener {
        @TxListener
        fun handleEvent(
            privateDataEvent: PrivateDataEvent<String>
        )
    }

    @TestConfiguration
    class ListenerConfig {

        @Bean
        fun listener() = mockk<StringPrivateDataEventListener>(relaxed = true)
    }
}
