package com.wavesenterprise.we.tx.observer.starter.observer.executor

import com.wavesenterprise.sdk.node.client.http.tx.CallContractTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.node.test.data.Util
import com.wavesenterprise.we.tx.observer.core.spring.executor.ScheduledTxQueueCleaner
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.we.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.we.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.we.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.mockPartition
import com.wavesenterprise.we.tx.observer.starter.observer.config.ObjectMapperConfig
import com.wavesenterprise.we.tx.observer.starter.observer.util.ModelFactory.enqueuedTx
import com.wavesplatform.we.flyway.schema.starter.FlywaySchemaConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TestTransaction

@DataJpaTest(
    properties = [
        "tx-observer.queue-cleaner.archive-height-window = 50",
        "tx-observer.queue-cleaner.delete-batch-size = 1",
        "tx-observer.queue-cleaner.cleanCronExpression = 0 0 0 * * ?"
    ]
)
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        ObjectMapperConfig::class,
        DataSourceAutoConfiguration::class,
        TxObserverJpaAutoConfig::class,
        NodeBlockingServiceFactoryMockConfiguration::class,
        TxObserverStarterConfig::class,
        FlywaySchemaConfiguration::class,
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
internal class ScheduledTxQueueCleanerTest {

    @Autowired
    lateinit var scheduledTxQueueCleaner: ScheduledTxQueueCleaner

    @Autowired
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    @MockBean
    lateinit var syncInfoService: SyncInfoService

    @Autowired
    lateinit var txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository

    @BeforeEach
    fun setUp() {
        txQueuePartitionJpaRepository.saveAndFlush(mockPartition)
    }

    @Test
    fun `should clean READ EnqueuedTx from queue with blockHeight less than current-archiveHeight`() {
        `when`(syncInfoService.observerHeight()).thenReturn(200)
        val blockHeightToLeave: Long = 160
        val leftReadCount = 5
        val leftNewCount = 10

        (1..5).forEach { _ ->
            enqueueTx(EnqueuedTxStatus.READ, 140)
        }
        (1..leftNewCount).forEach { _ ->
            enqueueTx(EnqueuedTxStatus.NEW, 140)
        }
        (1..leftReadCount).forEach { _ ->
            enqueueTx(EnqueuedTxStatus.READ, blockHeightToLeave)
        }

        TestTransaction.flagForCommit()
        TestTransaction.end()

        scheduledTxQueueCleaner.cleanReadEnqueuedTx()

        val leftEnqueuedTx = enqueuedTxJpaRepository.findAll()
        assertEquals(leftReadCount + leftNewCount, leftEnqueuedTx.size)
        assertEquals(
            leftReadCount,
            leftEnqueuedTx.count { it.blockHeight == blockHeightToLeave && it.status == EnqueuedTxStatus.READ }
        )
        assertEquals(
            leftNewCount,
            leftEnqueuedTx.count { it.status == EnqueuedTxStatus.NEW }
        )
    }

    @AfterEach
    fun cleanup() {
        enqueuedTxJpaRepository.deleteAll()
        txQueuePartitionJpaRepository.deleteAll()
    }

    private fun enqueueTx(status: EnqueuedTxStatus, blockHeight: Long) {
        enqueuedTxJpaRepository.save(
            enqueuedTx(
                tx = TestDataFactory.callContractTx(id = TxId.fromByteArray(Util.randomBytesFromUUID())).toDto(),
                status = status,
                partition = mockPartition,
                blockHeight = blockHeight
            )
        )
    }
}
