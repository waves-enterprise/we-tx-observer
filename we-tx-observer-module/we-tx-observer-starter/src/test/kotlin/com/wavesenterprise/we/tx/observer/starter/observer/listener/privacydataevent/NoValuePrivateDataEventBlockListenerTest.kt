package com.wavesenterprise.we.tx.observer.starter.observer.listener.privacydataevent

import com.wavesenterprise.we.tx.observer.api.NoPayloadException
import com.wavesenterprise.we.tx.observer.api.privacy.PrivateDataEvent
import com.wavesenterprise.we.tx.observer.api.tx.TxListener
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.samplePolicyDataHashTx
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [NoValuePrivateDataEventBlockListenerTest.ListenerConfig::class])
class NoValuePrivateDataEventBlockListenerTest : AbstractPrivateEventBlockListenerTest() {

    @Autowired
    lateinit var privateDataEventListener: NothingPrivateDataEventListener

    @Test
    fun `should throw exception when trying to get payload`() {
        enqueue(samplePolicyDataHashTx)

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(mockPartitionId)

        val noValuePrivateDataEventCaptor = slot<PrivateDataEvent<Nothing>>()
        verify { privateDataEventListener.handleEvent(privateDataEvent = capture(noValuePrivateDataEventCaptor)) }
        assertThrows<NoPayloadException> {
            noValuePrivateDataEventCaptor.captured.payload
        }
        assertEquals(samplePolicyDataHashTx, noValuePrivateDataEventCaptor.captured.policyDataHashTx)
    }

    interface NothingPrivateDataEventListener {
        @TxListener
        fun handleEvent(
            privateDataEvent: PrivateDataEvent<Nothing>
        )
    }

    @TestConfiguration
    class ListenerConfig {

        @Bean
        fun listener() = mockk<NothingPrivateDataEventListener>(relaxed = true)
    }
}
