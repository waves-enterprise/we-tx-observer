package com.wavesenterprise.sdk.tx.observer.starter.observer.executor

import com.wavesenterprise.sdk.tx.observer.core.spring.executor.ScheduledPartitionPoller
import com.wavesenterprise.sdk.tx.observer.core.spring.partition.TxPartitionPoller
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class ScheduledPartitionPollerTest {

    @MockK
    lateinit var errorHandlingTxPartitionPoller: TxPartitionPoller

    @InjectMockKs
    lateinit var scheduledPartitionPoller: ScheduledPartitionPoller

    @Test
    fun `should poll latest actual partition while they are available`() {
        every {
            errorHandlingTxPartitionPoller.pollPartition()
        } returnsMany listOf("id1", "id2", "id3", null, "id4")

        scheduledPartitionPoller.pollWhileHavingActivePartitions()

        verify(exactly = 4) {
            errorHandlingTxPartitionPoller.pollPartition()
        }
        confirmVerified(errorHandlingTxPartitionPoller)
    }
}
