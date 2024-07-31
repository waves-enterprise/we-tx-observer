package com.wavesenterprise.sdk.tx.observer.starter.observer.listener.privacydataevent

import com.wavesenterprise.sdk.node.domain.privacy.Data
import com.wavesenterprise.sdk.node.domain.privacy.PolicyItemRequest
import com.wavesenterprise.sdk.tx.observer.api.privacy.PrivateDataEvent
import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.samplePolicyDataHashTx
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration
import java.util.Base64
import java.util.Optional

@ContextConfiguration(classes = [ByteArrayPrivateDataEventBlockListenerTest.ListenerConfig::class])
class ByteArrayPrivateDataEventBlockListenerTest : AbstractPrivateEventBlockListenerTest() {

    @Autowired
    lateinit var privateDataEventListener: ByteArrayPrivateDataEventListener

    @Test
    fun `should handle private data event with byte array payload`() {
        val randomBytes = byteArrayOf(
            49, 48, 48, 32, -48, -79,
            -48, -80, -48, -70, -47,
            -127, -48, -66, -48, -78,
            32, -48, -65, -48, -75,
            -47, -128, -48, -78, -48,
            -66, -48, -68, -47, -125,
            32, -48, -70, -47, -126,
            -48, -66, 32, -48, -67,
            -48, -80, -48, -71, -48,
            -76, -47, -111, -47, -126,
        )
        every {
            privacyService.data(
                request = PolicyItemRequest(
                    policyId = samplePolicyDataHashTx.policyId,
                    dataHash = samplePolicyDataHashTx.dataHash,
                ),
            )
        } returns Optional.of(Data(Base64.getEncoder().encode(randomBytes)))

        enqueue(samplePolicyDataHashTx)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val byteArrayValuePrivateDataEventCaptor = slot<PrivateDataEvent<ByteArray>>()
        every { privateDataEventListener.handleEvent(capture(byteArrayValuePrivateDataEventCaptor)) } just Runs
        verify { privateDataEventListener.handleEvent(capture(byteArrayValuePrivateDataEventCaptor)) }
        assertArrayEquals(randomBytes, byteArrayValuePrivateDataEventCaptor.captured.payload)
        assertEquals(samplePolicyDataHashTx, byteArrayValuePrivateDataEventCaptor.captured.policyDataHashTx)
    }

    interface ByteArrayPrivateDataEventListener {
        @TxListener
        fun handleEvent(
            privateDataEvent: PrivateDataEvent<ByteArray>,
        )
    }

    @TestConfiguration
    class ListenerConfig {

        @Bean
        fun listener() = mockk<ByteArrayPrivateDataEventListener>(relaxed = true)
    }
}
