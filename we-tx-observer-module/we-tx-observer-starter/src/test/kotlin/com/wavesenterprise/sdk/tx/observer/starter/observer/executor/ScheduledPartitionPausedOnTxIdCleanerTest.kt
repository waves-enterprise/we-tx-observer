package com.wavesenterprise.sdk.tx.observer.starter.observer.executor

import com.wavesenterprise.sdk.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.sdk.node.client.http.tx.AtomicInnerTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.sdk.tx.observer.domain.TxQueuePartition
import com.wavesenterprise.sdk.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.sdk.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.sdk.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.sdk.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.sdk.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.ObjectMapperConfig
import com.wavesenterprise.sdk.tx.observer.starter.observer.util.ModelFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

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
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ScheduledPartitionPausedOnTxIdCleanerTest {

    @Autowired
    lateinit var txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository

    @PersistenceContext
    lateinit var em: EntityManager

    @Autowired
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    private val callContractTx = TestDataFactory.callContractTx().toDto()
    private val createContractTx = TestDataFactory.createContractTx().toDto()

    @Test
    fun `should clear pausedOnTxId where tx is not NEW`() {
        val samplePartition =
            TxQueuePartition(
                id = "partitionId",
                priority = 0,
                pausedOnTxId = callContractTx.id,
            )

        val samplePostponedEnqueuedTx = ModelFactory.enqueuedTx(
            tx = callContractTx,
            partition = samplePartition,
            status = EnqueuedTxStatus.POSTPONED,
        )
        txQueuePartitionJpaRepository.saveAndFlush(samplePartition)
        enqueuedTxJpaRepository.saveAndFlush(samplePostponedEnqueuedTx)

        val cleanedCount = txQueuePartitionJpaRepository.clearPausedOnTxIds()
        em.flushAndClear()

        assertEquals(1, cleanedCount)
        txQueuePartitionJpaRepository.findById(samplePartition.id).also {
            assertTrue(it.isPresent)
            assertNull(it.get().pausedOnTxId)
        }
    }

    @Test
    fun `should clear pausedOnTxId where tx is not exist`() {
        val samplePartition =
            TxQueuePartition(
                id = "partitionId",
                priority = 0,
                pausedOnTxId = callContractTx.id,
            )

        val samplePostponedEnqueuedTx = ModelFactory.enqueuedTx(
            tx = createContractTx,
            partition = samplePartition,
            status = EnqueuedTxStatus.POSTPONED,
        )
        txQueuePartitionJpaRepository.saveAndFlush(samplePartition)
        enqueuedTxJpaRepository.saveAndFlush(samplePostponedEnqueuedTx)

        val cleanedCount = txQueuePartitionJpaRepository.clearPausedOnTxIds()
        em.flushAndClear()

        assertEquals(1, cleanedCount)
        assertEquals(1, cleanedCount)
        txQueuePartitionJpaRepository.findById(samplePartition.id).also {
            assertTrue(it.isPresent)
            assertNull(it.get().pausedOnTxId)
        }
    }

    private fun EntityManager.flushAndClear() {
        flush()
        clear()
    }
}
