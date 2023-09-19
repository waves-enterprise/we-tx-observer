package com.wavesenterprise.we.tx.observer.starter.observer.listener.privacydataevent

import com.wavesenterprise.sdk.node.domain.privacy.DataComment
import com.wavesenterprise.sdk.node.domain.privacy.PolicyItemRequest
import com.wavesenterprise.we.tx.observer.api.privacy.MessageFilter
import com.wavesenterprise.we.tx.observer.api.privacy.PrivateDataEvent
import com.wavesenterprise.we.tx.observer.api.tx.TxListener
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.fileInfo
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.privacyInfoResponse
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.samplePolicyDataHashTx
import io.mockk.clearMocks
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
import java.util.Optional

@ContextConfiguration(classes = [FilteredByMetaPrivateDataEventBlockListenerTest.ListenerConfig::class])
class FilteredByMetaPrivateDataEventBlockListenerTest : AbstractPrivateEventBlockListenerTest() {

    @Autowired
    lateinit var privateDataEventListener: PrivateDataEventListener

    @Test
    fun `should handle private data event filtered by meta key`() {
        every {
            privacyService.info(
                request = PolicyItemRequest(
                    policyId = samplePolicyDataHashTx.policyId,
                    dataHash = samplePolicyDataHashTx.dataHash,
                )
            )
        } returns Optional.of(
            privacyInfoResponse.copy(
                info = fileInfo.copy(
                    comment = DataComment(objectMapper.writeValueAsString(mapOf("filter_key" to "desired_value")))
                )
            )
        )
        enqueue(samplePolicyDataHashTx)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val privateDataEventCaptor = slot<PrivateDataEvent<Nothing>>()
        verify { privateDataEventListener.handleEvent(privateDataEvent = capture(privateDataEventCaptor)) }
        assertEquals(samplePolicyDataHashTx, privateDataEventCaptor.captured.policyDataHashTx)
    }

    @Test
    fun `should not handle private data event with another meta value in filter key`() {
        clearMocks(privateDataEventListener)
        every {
            privacyService.info(
                request = PolicyItemRequest(
                    policyId = samplePolicyDataHashTx.policyId,
                    dataHash = samplePolicyDataHashTx.dataHash,
                )
            )
        } returns Optional.of(
            privacyInfoResponse.copy(
                info = fileInfo.copy(
                    comment = DataComment(objectMapper.writeValueAsString(mapOf("filter_key" to "not_desired_value")))
                )
            )
        )
        enqueue(samplePolicyDataHashTx)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        verify(exactly = 0) { privateDataEventListener.handleEvent(any()) }
    }

    @Test
    fun `should not handle private data event without meta`() {
        clearMocks(privateDataEventListener)
        every {
            privacyService.info(
                request = PolicyItemRequest(
                    policyId = samplePolicyDataHashTx.policyId,
                    dataHash = samplePolicyDataHashTx.dataHash,
                )
            )
        } returns Optional.of(privacyInfoResponse)

        enqueue(samplePolicyDataHashTx)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        verify(exactly = 0) { privateDataEventListener.handleEvent(any()) }
    }

    interface PrivateDataEventListener {
        @TxListener
        fun handleEvent(
            @MessageFilter(metaKey = "filter_key", metaKeyValue = "desired_value")
            privateDataEvent: PrivateDataEvent<Nothing>
        )
    }

    @TestConfiguration
    class ListenerConfig {

        @Bean
        fun listener() = mockk<PrivateDataEventListener>(relaxed = true)
    }
}
