package com.wavesenterprise.sdk.tx.observer.starter.observer.executor

import com.wavesenterprise.sdk.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.sdk.node.client.http.tx.AtomicInnerTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.tx.observer.domain.TxQueuePartition
import com.wavesenterprise.sdk.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.sdk.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.sdk.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.sdk.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.sdk.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.ObjectMapperConfig
import com.wavesenterprise.sdk.tx.observer.starter.observer.util.ModelFactory
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        ObjectMapperConfig::class,
        DataSourceAutoConfiguration::class,
        NodeBlockingServiceFactoryMockConfiguration::class,
        TxObserverJpaAutoConfig::class,
        TxObserverStarterConfig::class,
        FlywaySchemaConfiguration::class,
        TxObserverJpaConfig::class,
    ],
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ScheduledPartitionCleanerTest {

    @Autowired
    lateinit var txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository

    @Autowired
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    @PersistenceContext
    lateinit var em: EntityManager

    @Test
    fun `should delete empty partitions`() {
        val batchSize = 1
        val activePartition =
            TxQueuePartition(
                id = "activePartitionId",
                priority = 0,
            )
        val inactivePartition =
            TxQueuePartition(
                id = "inactivePartitionId",
                priority = 0,
            )
        val tx1 = ModelFactory.enqueuedTx(
            tx = TestDataFactory.callContractTx().toDto(),
            partition = activePartition,
        )
        val tx2 = ModelFactory.enqueuedTx(
            tx = TestDataFactory.callContractTx().toDto(),
            partition = activePartition,
        )
        txQueuePartitionJpaRepository.saveAndFlush(activePartition)
        txQueuePartitionJpaRepository.saveAndFlush(inactivePartition)
        enqueuedTxJpaRepository.saveAndFlush(tx1)
        enqueuedTxJpaRepository.saveAndFlush(tx2)
        em.flushAndClear()

        val deletedCount = txQueuePartitionJpaRepository.deleteEmptyPartitions(limit = batchSize)
        em.flushAndClear()

        assertEquals(1, deletedCount)
        assertTrue(txQueuePartitionJpaRepository.existsById(activePartition.id))
        assertFalse(txQueuePartitionJpaRepository.existsById(inactivePartition.id))
    }

    private fun EntityManager.flushAndClear() {
        flush()
        clear()
    }
}
