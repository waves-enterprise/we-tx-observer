package com.wavesenterprise.sdk.tx.observer.starter.observer.listener.privacydataevent

import com.wavesenterprise.sdk.node.domain.privacy.DataComment
import com.wavesenterprise.sdk.node.domain.privacy.PolicyItemRequest
import com.wavesenterprise.sdk.tx.observer.api.privacy.MessageFilter
import com.wavesenterprise.sdk.tx.observer.api.privacy.MessageFilters
import com.wavesenterprise.sdk.tx.observer.api.privacy.PrivateDataEvent
import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.fileInfo
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.privacyInfoResponse
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.samplePolicyDataHashTx
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

@ContextConfiguration(classes = [FilteredByMultipleMetaKeysPrivateDataEventBlockListenerTest.ListenerConfig::class])
class FilteredByMultipleMetaKeysPrivateDataEventBlockListenerTest : AbstractPrivateEventBlockListenerTest() {

    @Autowired
    lateinit var privateDataEventListener: PrivateDataEventListener

    @Test
    fun `should handle private data event filtered by multiple meta keys`() {
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
                    comment = DataComment(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "filter_key_one" to "desired_value_one",
                                "filter_key_two" to "desired_value_two",
                                "filter_key_three" to "desired_value_three"
                            )
                        )
                    )
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
    fun `should not handle other private data events`() {
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
                    comment = DataComment(
                        objectMapper.writeValueAsString(
                            mapOf(
                                "filter_key_one" to "not_desired_value_one",
                                "filter_key_two" to "not_desired_value_two",
                                "filter_key_three" to "not_desired_value_three"
                            )
                        )
                    )
                )
            )
        )
        enqueue(samplePolicyDataHashTx)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        verify(exactly = 0) { privateDataEventListener.handleEvent(any()) }
    }

    interface PrivateDataEventListener {
        @TxListener
        fun handleEvent(
            @MessageFilters(
                MessageFilter(metaKey = "filter_key_one", metaKeyValue = "desired_value_one"),
                MessageFilter(metaKey = "filter_key_two", metaKeyValue = "desired_value_two")
            )
            @MessageFilter(metaKey = "filter_key_three", metaKeyValue = "desired_value_three")
            privateDataEvent: PrivateDataEvent<Nothing>
        )
    }

    @TestConfiguration
    class ListenerConfig {

        @Bean
        fun listener() = mockk<PrivateDataEventListener>(relaxed = true)
    }
}
