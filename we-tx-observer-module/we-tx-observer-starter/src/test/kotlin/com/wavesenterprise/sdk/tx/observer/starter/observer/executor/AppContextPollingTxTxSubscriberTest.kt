package com.wavesenterprise.sdk.tx.observer.starter.observer.executor

import com.ninjasquad.springmockk.MockkBean
import com.wavesenterprise.sdk.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.sdk.node.client.http.tx.CreateContractTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.client.http.tx.PolicyDataHashTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.tx.observer.common.tx.subscriber.TxSubscriber
import com.wavesenterprise.sdk.tx.observer.core.spring.partition.PartitionHandler
import com.wavesenterprise.sdk.tx.observer.core.spring.partition.PollingTxSubscriber
import com.wavesenterprise.sdk.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.sdk.tx.observer.domain.TxQueuePartition
import com.wavesenterprise.sdk.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.sdk.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.sdk.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.sdk.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.sdk.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.sampleCreateContractTx
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.samplePolicyDataHashTx
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.ObjectMapperConfig
import com.wavesenterprise.sdk.tx.observer.starter.observer.util.ModelFactory.enqueuedTx
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.repository.findByIdOrNull
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
internal class AppContextPollingTxTxSubscriberTest {

    @Autowired
    lateinit var pollingTxSubscriber: PollingTxSubscriber

    @Autowired
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    @Autowired
    lateinit var txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository

    @PersistenceContext
    lateinit var em: EntityManager

    @MockkBean(relaxed = true)
    lateinit var mockTxSubscriber: TxSubscriber

    @MockkBean(relaxed = true)
    lateinit var partitionHandler: PartitionHandler

    private val samplePartition = TxQueuePartition(
        id = "partitionId",
        priority = 0
    )

    @BeforeEach
    fun setUp() {
        txQueuePartitionJpaRepository.saveAndFlush(samplePartition)
    }

    @Test
    fun `should dequeue Tx list, send to subscribers and mark Tx in this list as read, last is partition handled`() {
        val totalEnqueuedElements = 10
        val txListCaptor = mutableListOf<Tx>()

        (1..totalEnqueuedElements).map {
            enqueuedTxJpaRepository.save(
                enqueuedTx(
                    tx = sampleCreateContractTx.copy(id = TxId.fromByteArray("$it-$it".toByteArray())).toDto(),
                    positionInBlock = it,
                    partition = samplePartition,
                )
            )
        }

        TestTransaction.flagForCommit()
        TestTransaction.end()

        val elementsRead = pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(samplePartition.id)

        verify(exactly = totalEnqueuedElements) { mockTxSubscriber.subscribe(capture(txListCaptor)) }
        assertEquals(totalEnqueuedElements, elementsRead)
        assertTrue(enqueuedTxJpaRepository.findAll().all { it.status == EnqueuedTxStatus.READ })
        (1..totalEnqueuedElements).forEach {
            Assertions.assertArrayEquals("$it-$it".toByteArray(), txListCaptor[it - 1].id.bytes)
        }
        verify { partitionHandler.handleSuccessWhenReading(samplePartition.id, totalEnqueuedElements.toLong()) }
    }

    @Test
    fun `should deque only TX having status NEW`() {
        val totalEnqueuedElements = 10
        val txListCaptor = mutableListOf<Tx>()

        (1..totalEnqueuedElements).map {
            enqueuedTxJpaRepository.save(
                enqueuedTx(
                    tx = sampleCreateContractTx.copy(id = TxId.fromByteArray("$it-$it".toByteArray())).toDto(),
                    positionInBlock = it,
                    partition = samplePartition,
                    status = EnqueuedTxStatus.NEW
                )
            )
        }
        enqueuedTxJpaRepository.save(
            enqueuedTx(
                tx = sampleCreateContractTx.copy(id = TxId.fromByteArray("11-11".toByteArray())).toDto(),
                positionInBlock = 11,
                partition = samplePartition,
                status = EnqueuedTxStatus.READ
            )
        )

        TestTransaction.flagForCommit()
        TestTransaction.end()

        val elementsRead = pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(samplePartition.id)

        verify(exactly = 10) { mockTxSubscriber.subscribe(capture(txListCaptor)) }
        assertEquals(totalEnqueuedElements, elementsRead)
        verify { partitionHandler.handleSuccessWhenReading(samplePartition.id, totalEnqueuedElements.toLong()) }
    }

    @Test
    fun `should finish reading partition with success on first not available tx`() {
        val firstReadTx = sampleCreateContractTx.copy(id = TxId.fromByteArray("1".toByteArray())).toDto()
        val secondReadTx = sampleCreateContractTx.copy(id = TxId.fromByteArray("2".toByteArray())).toDto()
        val notAvailablePdhTx = samplePolicyDataHashTx.copy(id = TxId.fromByteArray("3".toByteArray())).toDto()
        val notReadTxAfterNotAvailable = sampleCreateContractTx.copy(id = TxId.fromByteArray("4".toByteArray())).toDto()
        var i = 1
        val txListCaptor = mutableListOf<Tx>()
        enqueuedTxJpaRepository.saveAll(
            listOf(
                enqueuedTx(
                    tx = firstReadTx,
                    positionInBlock = i++,
                    partition = samplePartition
                ),
                enqueuedTx(
                    tx = secondReadTx,
                    positionInBlock = i++,
                    partition = samplePartition
                ),
                enqueuedTx(
                    tx = notAvailablePdhTx,
                    positionInBlock = i++,
                    partition = samplePartition,
                    available = false
                ),
                enqueuedTx(
                    tx = notReadTxAfterNotAvailable,
                    positionInBlock = i,
                    partition = samplePartition
                )
            )
        )

        TestTransaction.flagForCommit()
        TestTransaction.end()

        val elementsRead = pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(samplePartition.id)

        verify(exactly = 2) { mockTxSubscriber.subscribe(capture(txListCaptor)) }
        assertEquals(2, elementsRead)

        txListCaptor.map { it.id.asBase58String() }.intersect(setOf(firstReadTx.id, secondReadTx.id)).apply {
            assertEquals(2, size)
        }
        verify { partitionHandler.pausePartitionOnTx(samplePartition.id, notAvailablePdhTx.id) }
        verify { partitionHandler.handleSuccessWhenReading(samplePartition.id, 2) }

        enqueuedTxJpaRepository.findByIdOrNull(firstReadTx.id)!!.apply {
            assertEquals(EnqueuedTxStatus.READ, status)
        }
        enqueuedTxJpaRepository.findByIdOrNull(secondReadTx.id)!!.apply {
            assertEquals(EnqueuedTxStatus.READ, status)
        }
        enqueuedTxJpaRepository.findByIdOrNull(notAvailablePdhTx.id)!!.apply {
            assertEquals(EnqueuedTxStatus.NEW, status)
        }
        enqueuedTxJpaRepository.findByIdOrNull(notReadTxAfterNotAvailable.id)!!.apply {
            assertEquals(EnqueuedTxStatus.NEW, status)
        }
    }

    @Test
    fun `should do nothing with single not available tx in partition`() {
        val notAvailableTx = TestDataFactory.createContractTx().toDto()
        enqueuedTxJpaRepository.save(
            enqueuedTx(
                tx = notAvailableTx,
                partition = samplePartition,
                available = false
            )
        )

        TestTransaction.flagForCommit()
        TestTransaction.end()

        pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(samplePartition.id).also {
            assertEquals(0, it)
        }
        verify { partitionHandler.pausePartitionOnTx(samplePartition.id, notAvailableTx.id) }
        enqueuedTxJpaRepository.findByIdOrNull(notAvailableTx.id)!!.apply {
            assertEquals(EnqueuedTxStatus.NEW, status)
        }
    }

    @AfterEach
    fun cleanup() {
        enqueuedTxJpaRepository.deleteAll()
        txQueuePartitionJpaRepository.deleteAll()
    }
}
