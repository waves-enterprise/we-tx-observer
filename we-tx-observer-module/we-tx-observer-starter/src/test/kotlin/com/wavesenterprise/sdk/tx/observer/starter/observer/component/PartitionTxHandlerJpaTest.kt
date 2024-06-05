package com.wavesenterprise.sdk.tx.observer.starter.observer.component

import com.wavesenterprise.sdk.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.sdk.node.client.http.tx.CreateContractTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.tx.observer.core.spring.partition.PartitionHandlerJpa
import com.wavesenterprise.sdk.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.sdk.tx.observer.domain.TxQueuePartition
import com.wavesenterprise.sdk.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.sdk.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.sdk.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.sdk.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.sdk.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.ObjectMapperConfig
import com.wavesenterprise.sdk.tx.observer.starter.observer.util.ModelFactory.enqueuedTx
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TestTransaction
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        ObjectMapperConfig::class,
        NodeBlockingServiceFactoryMockConfiguration::class,
        DataSourceAutoConfiguration::class,
        TxObserverJpaAutoConfig::class,
        TxObserverStarterConfig::class,
        TxObserverJpaConfig::class,
        FlywaySchemaConfiguration::class,
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
internal class PartitionTxHandlerJpaTest {

    @PersistenceContext
    lateinit var em: EntityManager

    @Autowired
    lateinit var txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository

    @Autowired
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    @Autowired
    lateinit var partitionHandler: PartitionHandlerJpa

    @Test
    fun `should decrease partition priority for error when reading partition`() {
        val samplePartition = TxQueuePartition(
            id = "partitionId",
            priority = 0
        )
        txQueuePartitionJpaRepository.saveAndFlush(samplePartition)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        partitionHandler.handleErrorWhenReading(samplePartition.id)

        val foundAfterDecrease = txQueuePartitionJpaRepository.findByIdOrNull(samplePartition.id)!!
        foundAfterDecrease.apply {
            assertEquals(samplePartition.priority - 1, priority)
        }
    }

    @Test
    fun `should reset partition priority`() {
        val samplePartition = TxQueuePartition(
            id = "partitionId",
            priority = -1
        )
        txQueuePartitionJpaRepository.saveAndFlush(samplePartition)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        partitionHandler.handleSuccessWhenReading(samplePartition.id, 15L)

        val foundAfterDecrease = txQueuePartitionJpaRepository.findByIdOrNull(samplePartition.id)!!
        foundAfterDecrease.apply {
            assertEquals(0, priority)
        }
    }

    @Test
    fun `should pause partition for tx`() {
        val txId = TxId.fromBase58("DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg")
        val samplePartition = TxQueuePartition(
            id = "partitionId",
            priority = 0,
        )
        txQueuePartitionJpaRepository.saveAndFlush(samplePartition)
        enqueuedTxJpaRepository.saveAndFlush(
            enqueuedTx(
                status = EnqueuedTxStatus.NEW,
                tx = TestDataFactory.createContractTx(id = txId).toDto(),
                partition = samplePartition,
                available = false,
            )
        )
        TestTransaction.flagForCommit()
        TestTransaction.end()

        partitionHandler.pausePartitionOnTx(partitionId = samplePartition.id, pausedOnTxId = txId.asBase58String())

        txQueuePartitionJpaRepository.findByIdOrNull(samplePartition.id)!!.apply {
            assertEquals(txId.asBase58String(), pausedOnTxId)
        }
    }

    @Test
    fun `should not pause partition for tx when it has already become available`() {
        val txId = TxId.fromBase58("DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg")
        val samplePartition = TxQueuePartition(
            id = "partitionId",
            priority = 0,
        )
        txQueuePartitionJpaRepository.saveAndFlush(samplePartition)
        enqueuedTxJpaRepository.saveAndFlush(
            enqueuedTx(
                status = EnqueuedTxStatus.NEW,
                tx = TestDataFactory.createContractTx(id = txId).toDto(),
                partition = samplePartition,
                available = true,
            )
        )
        TestTransaction.flagForCommit()
        TestTransaction.end()

        partitionHandler.pausePartitionOnTx(partitionId = samplePartition.id, pausedOnTxId = txId.asBase58String())

        txQueuePartitionJpaRepository.findByIdOrNull(samplePartition.id)!!.apply {
            assertNull(pausedOnTxId)
        }
    }

    @Test
    fun `should resume partition for tx`() {
        val txId = TxId.fromBase58("DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg")
        val samplePartition = TxQueuePartition(
            id = "partitionId",
            priority = 0,
            pausedOnTxId = txId.asBase58String(),
        )
        txQueuePartitionJpaRepository.saveAndFlush(samplePartition)
        enqueuedTxJpaRepository.saveAndFlush(
            enqueuedTx(
                status = EnqueuedTxStatus.NEW,
                tx = TestDataFactory.createContractTx(id = txId).toDto(),
                partition = samplePartition
            )
        )
        TestTransaction.flagForCommit()
        TestTransaction.end()

        partitionHandler.resumePartitionForTx(partitionId = samplePartition.id, txId = txId.asBase58String())

        txQueuePartitionJpaRepository.findByIdOrNull(samplePartition.id)!!.apply {
            assertNull(pausedOnTxId)
        }
    }

    @Test
    fun `should not resume partition for other tx specified`() {
        val txId = TxId.fromBase58("DP5MggKC8GJuLZshCVNSYwBtE6WTRtMM1YPPdcmwbuNg")
        val otherPausedOnTxId = TxId.fromBase58("C2HM9q3QzGSBydnCA4GMcf3cFnTaSuwaWXVtsCSTSmZW")
        val samplePartition = TxQueuePartition(
            id = "partitionId",
            priority = 0,
            pausedOnTxId = otherPausedOnTxId.asBase58String(),
        )
        txQueuePartitionJpaRepository.saveAndFlush(samplePartition)
        enqueuedTxJpaRepository.saveAndFlush(
            enqueuedTx(
                status = EnqueuedTxStatus.NEW,
                tx = TestDataFactory.createContractTx(id = txId).toDto(),
                partition = samplePartition,
            )
        )
        TestTransaction.flagForCommit()
        TestTransaction.end()

        partitionHandler.resumePartitionForTx(partitionId = samplePartition.id, txId = txId.asBase58String())

        txQueuePartitionJpaRepository.findByIdOrNull(samplePartition.id)!!.apply {
            assertEquals(otherPausedOnTxId.asBase58String(), pausedOnTxId)
        }
    }

    @AfterEach
    fun cleanup() {
        enqueuedTxJpaRepository.deleteAll()
        txQueuePartitionJpaRepository.deleteAll()
    }
}
