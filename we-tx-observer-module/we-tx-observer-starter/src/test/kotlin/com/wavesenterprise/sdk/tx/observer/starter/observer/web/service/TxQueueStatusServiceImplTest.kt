package com.wavesenterprise.sdk.tx.observer.starter.observer.web.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue
import com.wavesenterprise.sdk.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.node.client.blocking.tx.TxService
import com.wavesenterprise.sdk.node.client.http.tx.AtomicTxDto
import com.wavesenterprise.sdk.node.client.http.tx.CreateContractTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.client.http.tx.PolicyDataHashTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.domain.Address
import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.atomic.AtomicBadge
import com.wavesenterprise.sdk.node.domain.tx.AtomicTx
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.node.test.data.Util.Companion.randomBytesFromUUID
import com.wavesenterprise.sdk.tx.observer.common.jpa.util.flushAndClear
import com.wavesenterprise.sdk.tx.observer.core.spring.web.service.TxQueueService
import com.wavesenterprise.sdk.tx.observer.domain.TxQueuePartition
import com.wavesenterprise.sdk.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.sdk.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.sdk.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.sdk.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.sdk.tx.observer.starter.BlockInfoSynchronizerConfig
import com.wavesenterprise.sdk.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.sampleCreateContractTx
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.samplePolicyDataHashTx
import com.wavesenterprise.sdk.tx.observer.starter.observer.config.ObjectMapperConfig
import com.wavesenterprise.sdk.tx.observer.starter.observer.util.ModelFactory.enqueuedTx
import io.mockk.every
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import java.util.Optional

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        ObjectMapperConfig::class,
        DataSourceAutoConfiguration::class,
        TxObserverJpaAutoConfig::class,
        NodeBlockingServiceFactoryMockConfiguration::class,
        TxObserverStarterConfig::class,
        BlockInfoSynchronizerConfig::class,
        FlywaySchemaConfiguration::class,
        TxObserverJpaConfig::class,
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
internal class TxQueueStatusServiceImplTest {

    @Autowired
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    @Autowired
    lateinit var txQueuePartitionRepository: TxQueuePartitionJpaRepository

    @Autowired
    lateinit var txQueueStatusService: TxQueueService

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var nodeBlockingServiceFactory: NodeBlockingServiceFactory

    @Autowired
    lateinit var blocksService: BlocksService

    @Autowired
    lateinit var txService: TxService

    @PersistenceContext
    lateinit var em: EntityManager

    private val samplePartition = TxQueuePartition(
        id = "partitionId",
        priority = 0
    )

    @BeforeEach
    fun setUp() {
        txQueuePartitionRepository.saveAndFlush(samplePartition)
    }

    @Test
    fun `should return statistics for queue`() {
        var i = 0
        listOf(
            enqueuedTx(
                tx = sampleCreateContractTx.copy(id = TxId.fromByteArray("${i++}".toByteArray())).toDto(),
                partition = samplePartition
            ),
            enqueuedTx(
                tx = sampleCreateContractTx.copy(id = TxId.fromByteArray("${i++}".toByteArray())).toDto(),
                partition = samplePartition
            ),
            enqueuedTx(
                tx = samplePolicyDataHashTx.copy(id = TxId.fromByteArray("${i++}".toByteArray())).toDto(),
                partition = samplePartition
            ),
            enqueuedTx(
                tx = samplePolicyDataHashTx.copy(id = TxId.fromByteArray("${i++}".toByteArray())).toDto(),
                partition = samplePartition,
                available = false
            ),
            enqueuedTx(
                tx = samplePolicyDataHashTx.copy(id = TxId.fromByteArray("${i++}".toByteArray())).toDto(),
                partition = samplePartition,
                available = false
            )
        ).also {
            enqueuedTxJpaRepository.saveAll(it)
        }
        em.flushAndClear()

        val queueStatus = txQueueStatusService.getQueueStatus()

        queueStatus.privacyStatusApiDto.apply {
            assertEquals(totalNewPolicyDataHashes, 3)
            assertEquals(notAvailableCount, 2)
        }
    }

    @Test
    fun `should put to queue using blockSubscriber`() {
        val txId = TxId(randomBytesFromUUID())
        val innerTxId1 = TxId(randomBytesFromUUID())
        val innerTxId2 = TxId(randomBytesFromUUID())
        val senderAddress = Address("address".toByteArray())
        val atomicBadge = AtomicBadge(trustedSender = senderAddress)
        val enqueuedTxInfo = TestDataFactory.txInfo(
            tx = TestDataFactory.atomicTx(
                id = txId,
                txs = listOf(
                    TestDataFactory.createContractTx(
                        id = innerTxId1,
                        atomicBadge = atomicBadge,
                    ),
                    TestDataFactory.createContractTx(
                        id = innerTxId2,
                        atomicBadge = atomicBadge,
                    ),
                ),
                senderAddress = senderAddress,
            ),
            height = Height(1234),
        )

        every { txService.txInfo(txId) } returns Optional.of(enqueuedTxInfo)

        val addedTx = txQueueStatusService.addTxToQueueById(txId)

        addedTx.apply {
            assertEquals(txId.asBase58String(), id)
            val atomicTx = objectMapper.treeToValue<AtomicTxDto>(body)
            atomicTx.apply {
                assertEquals(txId.asBase58String(), id)
                assertEquals((enqueuedTxInfo.tx as AtomicTx).txs.size, transactions.size)
            }
        }
        assertNotNull(enqueuedTxJpaRepository.findByIdOrNull(innerTxId1.asBase58String()))
        assertNotNull(enqueuedTxJpaRepository.findByIdOrNull(innerTxId2.asBase58String()))
    }
}
