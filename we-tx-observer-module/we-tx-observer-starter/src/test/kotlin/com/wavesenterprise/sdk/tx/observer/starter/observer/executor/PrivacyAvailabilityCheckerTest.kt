package com.wavesenterprise.sdk.tx.observer.starter.observer.executor

import com.ninjasquad.springmockk.MockkBean
import com.wavesenterprise.sdk.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.node.client.http.tx.AtomicInnerTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.client.http.tx.CreateContractTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.tx.PolicyDataHashTx
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.tx.observer.api.privacy.PrivateContentResolver
import com.wavesenterprise.sdk.tx.observer.common.jpa.util.flushAndClear
import com.wavesenterprise.sdk.tx.observer.core.spring.component.OffsetProvider
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.PrivacyAvailabilityChecker
import com.wavesenterprise.sdk.tx.observer.core.spring.partition.PartitionHandler
import com.wavesenterprise.sdk.tx.observer.domain.TxQueuePartition
import com.wavesenterprise.sdk.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.sdk.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.sdk.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.sdk.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.sdk.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.ObjectMapperConfig
import com.wavesenterprise.sdk.tx.observer.starter.observer.util.ModelFactory.enqueuedTx
import io.mockk.every
import io.mockk.verify
import org.awaitility.Awaitility.await
import org.awaitility.Duration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TestTransaction
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@DataJpaTest(
    properties = [
        "tx-observer.privacy-check.limit-for-old = 2",
        "tx-observer.privacy-check.limit-for-recent = 3"
    ]
)
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        ObjectMapperConfig::class,
        DataSourceAutoConfiguration::class,
        TxObserverJpaAutoConfig::class,
        TxObserverStarterConfig::class,
        FlywaySchemaConfiguration::class,
        TxObserverJpaConfig::class,
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
internal class PrivacyAvailabilityCheckerTest {

    @Autowired
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    @Autowired
    lateinit var txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository

    @Autowired
    lateinit var privacyAvailabilityChecker: PrivacyAvailabilityChecker

    @MockkBean(relaxed = true)
    lateinit var partitionHandler: PartitionHandler

    @MockkBean(relaxed = true)
    lateinit var privateContentResolver: PrivateContentResolver

    @MockkBean(relaxed = true)
    lateinit var nodeBlockingServiceFactory: NodeBlockingServiceFactory

    @MockkBean(relaxed = true)
    lateinit var blocksService: BlocksService

    @MockkBean
    lateinit var offsetProvider: OffsetProvider

    @PersistenceContext
    lateinit var em: EntityManager

    private val samplePartition = TxQueuePartition(
        id = "partitionId",
        priority = 0
    )

    @BeforeEach
    fun setUp() {
        txQueuePartitionJpaRepository.saveAndFlush(samplePartition)
    }

    @AfterEach
    fun tearDown() {
        enqueuedTxJpaRepository.deleteAll()
        txQueuePartitionJpaRepository.deleteAll()
    }

    @ParameterizedTest
    @CsvSource(
        value = [ // upper bounds for offsets: recent = 10-3-2 = 5; old = 10-2 = 8
            "0,0", "0,1", "0,2", "0,3", "0,4", "0,5", "0,6", "0,7",
            "1,0", "1,1", "1,2", "1,3", "1,4", "1,5", "1,6", "1,7",
            "2,0", "2,1", "2,2", "2,3", "2,4", "2,5", "2,6", "2,7",
            "3,0", "3,1", "3,2", "3,3", "3,4", "3,5", "3,6", "3,7",
            "4,0", "4,1", "4,2", "4,3", "4,4", "4,5", "4,6", "4,7"
        ]
    )
    fun `should check availability for not available 114`(offsetForRecent: Int, offsetForOld: Int) {
        every { offsetProvider.provideOffset(any()) } returnsMany listOf(offsetForOld, offsetForRecent)

        (1..10).onEach {
            enqueuedTxJpaRepository.save(
                enqueuedTx(
                    tx = TestDataFactory.policyDataHashTx(id = TxId.fromByteArray("$it".toByteArray())).toDto(),
                    positionInBlock = it,
                    partition = samplePartition,
                    available = false
                )
            )
        }
        enqueuedTxJpaRepository.save(
            enqueuedTx(
                tx = TestDataFactory.policyDataHashTx(id = TxId.fromByteArray("11".toByteArray())).toDto(),
                positionInBlock = 11,
                partition = samplePartition
            )
        )
        (1..3).onEach {
            val id = it + 10
            enqueuedTxJpaRepository.save(
                enqueuedTx(
                    tx = TestDataFactory.createContractTx(id = TxId.fromByteArray("$id".toByteArray())).toDto(),
                    positionInBlock = it,
                    partition = samplePartition
                )
            )
        }
        em.flushAndClear()
        TestTransaction.flagForCommit()
        TestTransaction.end()

        privacyAvailabilityChecker.checkPrivacyAvailability()

        val checkAvailablePdhTxCaptor = mutableListOf<PolicyDataHashTx>()
        verify(exactly = 5) { privateContentResolver.isAvailable(capture(checkAvailablePdhTxCaptor)) }
    }

    @Test
    fun `should resume partitions for transactions that became available`() {
        every { offsetProvider.provideOffset(any()) } returnsMany listOf(0, 0)
        val listOfTxIds = mutableListOf<TxId>().apply {
            repeat(5) { add(TestDataFactory.txId()) }
        }

        listOfTxIds.forEachIndexed { index, txId ->
            enqueuedTxJpaRepository.save(
                enqueuedTx(
                    tx = TestDataFactory.policyDataHashTx(id = txId).toDto(),
                    positionInBlock = index,
                    partition = samplePartition,
                    available = false
                )
            )
        }
        TestTransaction.flagForCommit()
        TestTransaction.end()
        val firstIdThatBecameAvailable = listOfTxIds[1]
        val secondIdThatBecameAvailable = listOfTxIds[3]
        every {
            privateContentResolver.isAvailable(
                match {
                    it.id in setOf(firstIdThatBecameAvailable, secondIdThatBecameAvailable)
                }
            )
        } returns true
        every {
            privateContentResolver.isAvailable(
                match {
                    it.id in setOf(firstIdThatBecameAvailable, secondIdThatBecameAvailable)
                }
            )
        } returns true

        privacyAvailabilityChecker.checkPrivacyAvailability()

        val resumeOnTxCaptor = mutableListOf<String>()
        await().atMost(Duration.TEN_SECONDS).untilAsserted {
            verify(exactly = 5) { privateContentResolver.isAvailable(any()) }
        }
        await().atMost(Duration.TEN_SECONDS).untilAsserted {
            verify(exactly = 2) {
                partitionHandler.resumePartitionForTx(
                    partitionId = eq(samplePartition.id),
                    txId = capture(resumeOnTxCaptor),
                )
            }
        }
        val resumedTxIds = resumeOnTxCaptor
        assertEquals(2, resumedTxIds.size)
        assertTrue(resumedTxIds.contains(firstIdThatBecameAvailable.asBase58String()))
        assertTrue(resumedTxIds.contains(secondIdThatBecameAvailable.asBase58String()))
        enqueuedTxJpaRepository.findAll()
            .filter { it.available }.map { it.id }.toSet()
            .apply {
                assertEquals(2, size)
                assertTrue(containsAll(resumedTxIds))
            }
    }

    @Test
    fun `should catch exception from private content resolver and continue processing 114 txs`() {
        val resumeOnTxCaptor = mutableListOf<String>()
        every { offsetProvider.provideOffset(any()) } returnsMany listOf(0, 0)
        (1..5).onEach {
            enqueuedTxJpaRepository.save(
                enqueuedTx(
                    tx = TestDataFactory.policyDataHashTx(id = TxId.fromByteArray("$it".toByteArray())).toDto(),
                    positionInBlock = it,
                    partition = samplePartition,
                    available = false
                )
            )
        }
        TestTransaction.flagForCommit()
        TestTransaction.end()

        every { privateContentResolver.isAvailable(any()) } returns
            true andThen true andThenThrows Exception("Exception") andThen true andThen true

        privacyAvailabilityChecker.checkPrivacyAvailability()

        verify {
            partitionHandler.resumePartitionForTx(
                partitionId = eq(samplePartition.id),
                txId = capture(resumeOnTxCaptor)
            )
        }
    }
}
