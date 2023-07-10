package com.wavesenterprise.we.tx.observer.starter.observer.listener.privacydataevent

import com.wavesenterprise.sdk.node.domain.PolicyName
import com.wavesenterprise.sdk.node.domain.privacy.PolicyItemRequest
import com.wavesenterprise.sdk.node.test.data.TestDataFactory.Companion.txInfo
import com.wavesenterprise.we.tx.observer.api.privacy.PolicyFilter
import com.wavesenterprise.we.tx.observer.api.privacy.PrivateDataEvent
import com.wavesenterprise.we.tx.observer.api.tx.TxListener
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.privacyInfoResponse
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.sampleCreatePolicyTx
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

@ContextConfiguration(classes = [FilteredByPolicyPrivateDataEventBlockListenerTest.ListenerConfig::class])
class FilteredByPolicyPrivateDataEventBlockListenerTest : AbstractPrivateEventBlockListenerTest() {

    @Autowired
    lateinit var privateDataEventListener: PrivateDataEventListener

    @Test
    fun `should handle private data event filtered by policy name`() {
        every {
            privacyService.info(
                request = PolicyItemRequest(
                    policyId = samplePolicyDataHashTx.policyId,
                    dataHash = samplePolicyDataHashTx.dataHash,
                )
            )
        } returns Optional.of(privacyInfoResponse)
        every {
            txService.txInfo(samplePolicyDataHashTx.policyId.txId)
        } returns Optional.of(
            txInfo(
                tx = sampleCreatePolicyTx.copy(policyName = PolicyName("policy_name_prefix_in_policy_name"))
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
        } returns Optional.of(privacyInfoResponse)
        enqueue(samplePolicyDataHashTx)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        verify(exactly = 0) { privateDataEventListener.handleEvent(any()) }
    }

    interface PrivateDataEventListener {
        @TxListener
        fun handleEvent(
            @PolicyFilter(namePrefix = "policy_name_prefix")
            privateDataEvent: PrivateDataEvent<Nothing>
        )
    }

    @TestConfiguration
    class ListenerConfig {

        @Bean
        fun listener() = mockk<PrivateDataEventListener>(relaxed = true)
    }
}
