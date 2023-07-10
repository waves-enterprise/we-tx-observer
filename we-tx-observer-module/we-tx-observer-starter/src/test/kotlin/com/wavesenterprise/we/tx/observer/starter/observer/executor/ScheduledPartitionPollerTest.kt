package com.wavesenterprise.we.tx.observer.starter.observer.executor

import com.wavesenterprise.we.tx.observer.core.spring.executor.ScheduledPartitionPoller
import com.wavesenterprise.we.tx.observer.core.spring.partition.LatestTxPartitionPoller
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
    lateinit var errorHandlingLatestTxPartitionPoller: LatestTxPartitionPoller

    @InjectMockKs
    lateinit var scheduledPartitionPoller: ScheduledPartitionPoller

    @Test
    fun `should poll latest actual partition while they are available`() {
        every {
            errorHandlingLatestTxPartitionPoller.pollLatestActualPartition()
        } returnsMany listOf("id1", "id2", "id3", null, "id4")

        scheduledPartitionPoller.pollWhileHavingActivePartitions()

        verify(exactly = 4) {
            errorHandlingLatestTxPartitionPoller.pollLatestActualPartition()
        }
        confirmVerified(errorHandlingLatestTxPartitionPoller)
    }
}
