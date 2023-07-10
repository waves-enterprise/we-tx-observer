package com.wavesenterprise.we.tx.observer.starter.observer.web.service

import com.wavesenterprise.sdk.node.client.http.tx.CreateContractTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.we.tx.observer.core.spring.web.service.TxQueueService
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.we.tx.observer.domain.TxQueuePartition
import com.wavesenterprise.we.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.we.tx.observer.jpa.config.TxObserverJpaConfig
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
import org.junit.jupiter.api.assertAll
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TestTransaction

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        ObjectMapperConfig::class,
        DataSourceAutoConfiguration::class,
        TxObserverJpaAutoConfig::class,
        NodeBlockingServiceFactoryMockConfiguration::class,
        TxObserverStarterConfig::class,
        FlywaySchemaConfiguration::class,
        TxObserverJpaConfig::class,
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
internal class TxQueueServiceTest(
    @Value("\${tx-observer.error-priority-offset:10}")
    private val errorPriorityOffset: Int
) {

    @Autowired
    lateinit var txQueueService: TxQueueService

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
    fun `should downgrade height and remove some tx-items`() {
        // ARRANGE
        val oldBlockHeight = 100L
        val txCountOnOldHeight = 4
        repeat(txCountOnOldHeight) {
            enqueuedTxJpaRepository.save(
                enqueuedTx(
                    tx = TestDataFactory.createContractTx(id = TxId.fromByteArray("id_old_$it".toByteArray())).toDto(),
                    positionInBlock = it,
                    blockHeight = oldBlockHeight,
                    partition = mockPartition
                )
            )
        }
        val txCountOnNewHeight = 6
        val downgradedBlockHeight = oldBlockHeight - 20
        repeat(txCountOnNewHeight) {
            enqueuedTxJpaRepository.save(
                enqueuedTx(
                    tx = TestDataFactory.createContractTx(id = TxId.fromByteArray("id_new_$it".toByteArray())).toDto(),
                    positionInBlock = it,
                    blockHeight = downgradedBlockHeight,
                    partition = mockPartition
                )
            )
        }
        val resetToHeight = downgradedBlockHeight + 1

        // ACT
        val removedTxCount = txQueueService.resetToHeightAndReturnDeletedTxCount(resetToHeight)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        // ASSERT
        verify(syncInfoService).resetTo(resetToHeight)
        assertEquals(txCountOnOldHeight, removedTxCount)
        assertEquals(txCountOnNewHeight, enqueuedTxJpaRepository.findAll().count())
    }

    @Test
    fun `should postpone errors`() {
        // ARRANGE
        val errorPartition = txQueuePartitionJpaRepository.saveAndFlush(
            TxQueuePartition(
                id = "errorPartId",
                priority = -errorPriorityOffset - 1
            )
        )

        val txCountError = 4
        (0..9).map {
            enqueuedTxJpaRepository.save(
                enqueuedTx(
                    tx = TestDataFactory.createContractTx(id = TxId.fromByteArray("tx_$it".toByteArray())).toDto(),
                    positionInBlock = it,
                    partition = if (it < txCountError) errorPartition else mockPartition
                )
            )
        }

        // ACT
        val postponedCount = txQueueService.postponeErrors()
        TestTransaction.flagForCommit()
        TestTransaction.end()

        // ASSERT
        val txs = enqueuedTxJpaRepository.findAll()
        assertAll(
            listOf {
                assertEquals(txCountError, postponedCount, "Count of Tx with status POSTPONED must be equals $txCountError")
            }.union(
                txs.map {
                    if (it.partition.priority < -errorPriorityOffset) {
                        { assertEquals(EnqueuedTxStatus.POSTPONED, it.status, "Status of Tx ${it.id} in partition ${it.partition.id} with priority lower than -$errorPriorityOffset (actual ${it.partition.priority}) must be equals POSTPONED") }
                    } else {
                        { assertEquals(EnqueuedTxStatus.NEW, it.status, "Status of Tx ${it.id} in partition ${it.partition.id} with priority greater or equals -$errorPriorityOffset (actual ${it.partition.priority}) must be equals NEW") }
                    }
                }
            )
        )
    }

    @AfterEach
    fun cleanup() {
        enqueuedTxJpaRepository.deleteAll()
        txQueuePartitionJpaRepository.deleteAll()
    }
}
