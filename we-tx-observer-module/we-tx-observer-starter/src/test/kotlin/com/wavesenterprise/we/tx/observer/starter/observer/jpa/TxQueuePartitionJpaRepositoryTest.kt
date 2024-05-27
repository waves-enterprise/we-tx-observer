package com.wavesenterprise.we.tx.observer.starter.observer.jpa

import com.wavesenterprise.sdk.node.client.http.tx.CreateContractTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.domain.Timestamp
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.node.test.data.Util.Companion.randomBytesFromUUID
import com.wavesenterprise.we.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.we.tx.observer.common.jpa.util.TX_OBSERVER_SCHEMA_NAME
import com.wavesenterprise.we.tx.observer.common.tx.executor.TxExecutor
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTx
import com.wavesenterprise.we.tx.observer.domain.TxQueuePartition
import com.wavesenterprise.we.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.we.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.we.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.mockPartition
import com.wavesenterprise.we.tx.observer.starter.observer.util.ModelFactory.enqueuedTx
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.stream.Stream
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        DataSourceAutoConfiguration::class,
        TxObserverJpaAutoConfig::class,
        TxObserverJpaConfig::class,
        NodeBlockingServiceFactoryMockConfiguration::class,
        FlywaySchemaConfiguration::class,
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
internal class TxQueuePartitionJpaRepositoryTest {

    @PersistenceContext
    lateinit var em: EntityManager

    @Autowired
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    @Autowired
    lateinit var txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository

    @Autowired
    lateinit var txExecutor: TxExecutor

    @Test
    fun `should find one stuck partitions`() {
        val firstPartition = txQueuePartitionJpaRepository.save(mockPartition.copy(id = "part_id_1", priority = -200))
        val secondPartition = txQueuePartitionJpaRepository.save(mockPartition.copy(id = "part_id_2", priority = 0))
        txQueuePartitionJpaRepository.save(mockPartition.copy(id = "part_id_3", priority = -200))

        enqueuedTxJpaRepository.save(
            enqueuedTx(
                tx = TestDataFactory.createContractTx(id = TxId.fromByteArray("id_1".toByteArray())).toDto(),
                partition = firstPartition
            )
        )
        enqueuedTxJpaRepository.save(
            enqueuedTx(
                tx = TestDataFactory.createContractTx(id = TxId.fromByteArray("id_2".toByteArray())).toDto(),
                partition = secondPartition
            )
        )
        flushAndClear()
        assertThat(txQueuePartitionJpaRepository.countStuckPartitions(), `is`(1L))
    }

    @Test
    fun `should find two actual partition ids in desc priority order`() {
        val firstPartition = txQueuePartitionJpaRepository.save(mockPartition.copy(id = "part_id_1", priority = -200))
        val secondPartition = txQueuePartitionJpaRepository.save(mockPartition.copy(id = "part_id_2", priority = 0))
        txQueuePartitionJpaRepository.save(mockPartition.copy(id = "part_id_3", priority = -100))

        enqueuedTxJpaRepository.save(
            enqueuedTx(
                tx = TestDataFactory.createContractTx(id = TxId.fromByteArray("id_1".toByteArray())).toDto(),
                partition = firstPartition
            )
        )
        enqueuedTxJpaRepository.save(
            enqueuedTx(
                tx = TestDataFactory.createContractTx(id = TxId.fromByteArray("id_2".toByteArray())).toDto(),
                partition = secondPartition
            )
        )
        flushAndClear()

        var findActualPartitionQuery = em.createNativeQuery(
            """
                select p.* from $TX_OBSERVER_SCHEMA_NAME.tx_queue_partition p 
                    join tx_observer.enqueued_tx etx on etx.partition_id = p.id and etx.status = 'NEW'
                        where p.paused_on_tx_id is null
                    order by p.priority desc, etx.tx_timestamp
            """,
            TxQueuePartition::class.java
        )
        (findActualPartitionQuery.resultList as List<TxQueuePartition>).also { txQueuePartitionsList ->
            assertEquals(secondPartition.id, txQueuePartitionsList[0].id)
            assertEquals(firstPartition.id, txQueuePartitionsList[1].id)
        }
    }

    @Test
    fun `should find latest actual partition`() {
        val firstTxQueuePartition = partitionWithNewTx(
            id = "firstTxQueuePartitionId",
            priority = 0,
            enqueuedTxTimestamp = OffsetDateTime.of(
                LocalDate.now(),
                LocalTime.of(11, 0, 0),
                ZoneOffset.UTC
            )
        )
        val secondTxQueuePartition = partitionWithNewTx(
            id = "secondTxQueuePartitionId",
            priority = 0,
            enqueuedTxTimestamp = OffsetDateTime.of(
                LocalDate.now(),
                LocalTime.of(12, 0, 0),
                ZoneOffset.UTC
            )
        )
        val thirdTxQueuePartition = partitionWithNewTx(
            id = "thirdTxQueuePartitionId",
            priority = -1,
            enqueuedTxTimestamp = OffsetDateTime.of(
                LocalDate.now(),
                LocalTime.of(10, 0, 0),
                ZoneOffset.UTC
            )
        )
        setOf(
            firstTxQueuePartition,
            secondTxQueuePartition,
            thirdTxQueuePartition
        ).apply {
            map { it.first }.also { txQueuePartitionJpaRepository.saveAll(it) }
            map { it.second }.also { enqueuedTxJpaRepository.saveAll(it) }
        }
        flushAndClear()

        txQueuePartitionJpaRepository.findAndLockLatestPartition().also {
            assertEquals(firstTxQueuePartition.first.id, it)
        }
    }

    @Test
    fun `should find actual partition`() {
        val firstTxQueuePartition = partitionWithNewTx(
            id = "firstTxQueuePartitionId",
            priority = 0,
            enqueuedTxTimestamp = OffsetDateTime.of(
                LocalDate.now(),
                LocalTime.of(11, 0, 0),
                ZoneOffset.UTC
            )
        )
        val secondTxQueuePartition = partitionWithNewTx(
            id = "secondTxQueuePartitionId",
            priority = 0,
            enqueuedTxTimestamp = OffsetDateTime.of(
                LocalDate.now(),
                LocalTime.of(12, 0, 0),
                ZoneOffset.UTC
            )
        )
        setOf(
            firstTxQueuePartition,
            secondTxQueuePartition,
        ).apply {
            map { it.first }.also { txQueuePartitionJpaRepository.saveAll(it) }
            map { it.second }.also { enqueuedTxJpaRepository.saveAll(it) }
        }
        flushAndClear()

        txQueuePartitionJpaRepository.findAndLockRandomPartition().also {
            if (firstTxQueuePartition.first.id == it) {
                assertEquals(firstTxQueuePartition.first.id, it)
            } else {
                assertEquals(secondTxQueuePartition.first.id, it)
            }
        }
    }

    @Test
    fun `shouldn't delete empty partition when it locked`() {
        val txQueuePartition = txExecutor.requiresNew {
            txQueuePartitionJpaRepository.save(mockPartition)
        }
        flushAndClear()

        val latch = CountDownLatch(1)
        val lockThread = Thread {
            txExecutor.requiresNew {
                txQueuePartitionJpaRepository.findAndLockById(txQueuePartition.id)
                latch.countDown()
                Thread.sleep(1000)
            }
        }
        lockThread.start()
        latch.await()
        val deletedCount = txExecutor.requiresNew {
            txQueuePartitionJpaRepository.deleteEmptyPartitions(1)
        }
        lockThread.join()
        assertTrue(0 == deletedCount)
    }

    @ParameterizedTest
    @MethodSource("foundPartitions")
    fun `should find specified partitions`(
        partition: TxQueuePartition,
        enqueuedTx: EnqueuedTx
    ) {
        txQueuePartitionJpaRepository.saveAndFlush(partition)
        enqueuedTxJpaRepository.saveAndFlush(enqueuedTx)

        assertEquals(partition.id, txQueuePartitionJpaRepository.findAndLockLatestPartition())
    }

    @ParameterizedTest
    @MethodSource("notFoundPartitions")
    fun `shouldn't find any of the specified partitions`(
        partition: TxQueuePartition,
        enqueuedTx: EnqueuedTx?
    ) {
        txQueuePartitionJpaRepository.saveAndFlush(partition)
        enqueuedTx?.also { enqueuedTxJpaRepository.saveAndFlush(enqueuedTx) }

        assertNull(txQueuePartitionJpaRepository.findAndLockLatestPartition())
    }

    companion object {

        @JvmStatic
        private fun foundPartitions(): Stream<Arguments> =
            setOf(
                partitionWithNewTx(
                    id = "secondTxQueuePartitionId",
                    priority = 0,
                    enqueuedTxTimestamp = OffsetDateTime.of(
                        LocalDate.now(),
                        LocalTime.of(12, 0, 0),
                        ZoneOffset.UTC
                    )
                ),
                partitionWithNewTx(
                    id = "thirdTxQueuePartitionId",
                    priority = -1,
                    enqueuedTxTimestamp = OffsetDateTime.of(
                        LocalDate.now(),
                        LocalTime.of(10, 0, 0),
                        ZoneOffset.UTC
                    )
                ),
                partitionWithNewTx(
                    id = "forthTxQueuePartitionId",
                    priority = -1,
                    enqueuedTxTimestamp = OffsetDateTime.of(
                        LocalDate.now(),
                        LocalTime.of(14, 0, 0),
                        ZoneOffset.UTC
                    )
                )
            ).map { Arguments.of(it.first, it.second) }.stream()

        @JvmStatic
        private fun notFoundPartitions(): Stream<Arguments> =
            setOf(
                TxQueuePartition(
                    id = "notFoundPartitionIdWithoutTx",
                    priority = 0
                ) to null,
                partitionWithNewTx(
                    id = "actualButPausedPartitionId",
                    priority = 0,
                    pausedOnTxId = "someTxIdToBePauseOn",
                    enqueuedTxTimestamp = OffsetDateTime.of(
                        LocalDate.now(),
                        LocalTime.of(11, 0, 0),
                        ZoneOffset.UTC
                    )
                )
            ).map { Arguments.of(it.first, it.second) }.stream()

        private fun partitionWithNewTx(
            id: String,
            priority: Int,
            pausedOnTxId: String? = null,
            enqueuedTxTimestamp: OffsetDateTime
        ): Pair<TxQueuePartition, EnqueuedTx> {
            val partition = TxQueuePartition(
                id = id,
                priority = priority,
                pausedOnTxId = pausedOnTxId
            )
            return partition to enqueuedTx(
                tx = TestDataFactory.createContractTx(
                    id = TxId.fromByteArray(randomBytesFromUUID()),
                    timestamp = Timestamp(enqueuedTxTimestamp.toInstant().toEpochMilli())
                ).toDto(),
                partition = partition,
            )
        }
    }

    private fun flushAndClear() {
        em.apply {
            flush()
            clear()
        }
    }
}
