package com.wavesenterprise.we.tx.observer.starter.observer.executor

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ninjasquad.springmockk.MockkBean
import com.wavesenterprise.sdk.node.client.http.tx.TxDto
import com.wavesenterprise.sdk.node.domain.Address
import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Timestamp.Companion.toDateTimeFromUTCBlockChain
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.atomic.AtomicBadge
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.node.test.data.Util.Companion.randomBytesFromUUID
import com.wavesenterprise.we.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.we.tx.observer.api.block.WeBlockInfo
import com.wavesenterprise.we.tx.observer.api.partition.TxQueuePartitionResolver
import com.wavesenterprise.we.tx.observer.api.tx.TxEnqueuePredicate
import com.wavesenterprise.we.tx.observer.core.spring.component.HttpApiWeBlockInfo
import com.wavesenterprise.we.tx.observer.core.spring.executor.EnqueueingBlockSubscriber
import com.wavesenterprise.we.tx.observer.core.spring.metrics.AddableLongMetricsContainer
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.we.tx.observer.domain.TxQueuePartition
import com.wavesenterprise.we.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.we.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.we.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.we.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.sampleCallContractTx
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.sampleCreateContractTx
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.samplePolicyDataHashTx
import com.wavesenterprise.we.tx.observer.starter.observer.config.ObjectMapperConfig
import com.wavesenterprise.we.tx.observer.starter.observer.util.ModelFactory.blockAtHeight
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

@DataJpaTest(properties = ["tx-observer.default-partition-id = thatDefaultPartitionId"])
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
internal class EnqueueingBlockTxSubscriberTest {

    @Autowired
    lateinit var enqueueingBlockSubscriber: EnqueueingBlockSubscriber

    @Autowired
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    @Autowired
    lateinit var txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository

    @MockkBean(relaxed = true)
    lateinit var txEnqueuedPredicate: TxEnqueuePredicate

    @MockkBean(relaxed = true)
    lateinit var txQueuePartitionResolver: TxQueuePartitionResolver

    @MockkBean(name = "totalTxMetricsContainer", relaxed = true)
    lateinit var totalTxMetricsContainer: AddableLongMetricsContainer

    @MockkBean(name = "totalLogicalTxMetricsContainer", relaxed = true)
    lateinit var totalLogicalTxMetricsContainer: AddableLongMetricsContainer

    @MockkBean(name = "filteredTxMetricsContainer", relaxed = true)
    lateinit var filteredTxMetricContainer: AddableLongMetricsContainer

    @PersistenceContext
    lateinit var em: EntityManager

    val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun init() {
        enqueuedTxJpaRepository.deleteAll()
        txQueuePartitionJpaRepository.deleteAll()
    }

    @Test
//    @Disabled
    fun `should handle txList by persisting not existent tx and resolving partitions`() {
        val first = samplePolicyDataHashTx.copy(id = TxId.fromByteArray("1_1".toByteArray()))
        val second = sampleCreateContractTx.copy(id = TxId.fromByteArray("1_2".toByteArray()))
        val third = sampleCallContractTx.copy(id = TxId.fromByteArray("1_3".toByteArray()))
        val forth = sampleCallContractTx.copy(id = TxId.fromByteArray("1_4".toByteArray()))
        val mockTxList: List<Tx> = listOf(first, second, third, forth)
        every { txEnqueuedPredicate.isEnqueued(any()) } returns true
        val partFirstId = "part1"
        val partSecondId = "part2"
        val existingPart = TxQueuePartition(
            id = "existingPart3",
            priority = -1
        )
        txQueuePartitionJpaRepository.saveAndFlush(existingPart)
        every { txQueuePartitionResolver.resolvePartitionId(first) } returns partFirstId
        every { txQueuePartitionResolver.resolvePartitionId(second) } returns partSecondId
        every { txQueuePartitionResolver.resolvePartitionId(third) } returns existingPart.id
        every { txQueuePartitionResolver.resolvePartitionId(forth) } returns partFirstId
        val height = 123L

        enqueueingBlockSubscriber.subscribe(weBlockInfo(txList = mockTxList, height = Height(height)))

        em.flush()
        em.clear()

        val allEnqueuedTxs = enqueuedTxJpaRepository.findAll(Sort.by("positionInBlock"))
        val mockedTxIdSet = mockTxList.map { it.id.asBase58String() }.toSet()
        assertEquals(mockedTxIdSet.size, allEnqueuedTxs.size)
        val resultingTxIdSet = allEnqueuedTxs.map { it.id }.toSet()
        assertEquals(allEnqueuedTxs.size, resultingTxIdSet.size)
        assertEquals(mockedTxIdSet, resultingTxIdSet)
        assertTrue(allEnqueuedTxs.all { it.status == EnqueuedTxStatus.NEW })
        assertTrue(allEnqueuedTxs.all { it.blockHeight == height })
        assertEquals(0, allEnqueuedTxs.first().positionInBlock)
        assertEquals(mockTxList.size - 1, allEnqueuedTxs.last().positionInBlock)
        assertEquals(
            mockTxList.first().timestamp.toDateTimeFromUTCBlockChain().nano,
            allEnqueuedTxs.first().txTimestamp.nano
        )

        verify(exactly = mockTxList.size) { txEnqueuedPredicate.isEnqueued(any()) }
        verify(exactly = mockTxList.size) { txQueuePartitionResolver.resolvePartitionId(any()) }

        val allPartitions = txQueuePartitionJpaRepository.findAll()
        assertEquals(3, allPartitions.size)

        val partitionByIdMap = allPartitions.map { it.id to it }.toMap()
        assertNotNull(partitionByIdMap[partFirstId])
        assertNotNull(partitionByIdMap[partSecondId])
        (partitionByIdMap[existingPart.id] ?: error("No partition with ID = ${existingPart.id} found "))
            .apply {
                assertEquals(existingPart.priority, priority)
            }

        verify { totalTxMetricsContainer.add(4) }
        verify { filteredTxMetricContainer.add(4) }
    }

    @Test
    fun `should ignore existent txs`() {
        val mockTxList: List<Tx> = listOf(
            TestDataFactory.policyDataHashTx(id = TxId.fromByteArray("2_1".toByteArray())),
            TestDataFactory.callContractTx(id = TxId.fromByteArray("2_2".toByteArray()))
        )
        every { txEnqueuedPredicate.isEnqueued(any()) } returns true

        enqueueingBlockSubscriber.subscribe(weBlockInfo(txList = mockTxList, height = Height(1)))

        em.flush()
        em.clear()

        val allEnqueuedTxs = enqueuedTxJpaRepository.findAll()
        assertEquals(mockTxList.size, allEnqueuedTxs.size)
        verify { totalTxMetricsContainer.add(2) }
        verify { filteredTxMetricContainer.add(2) }
        clearMocks(totalTxMetricsContainer)
        clearMocks(filteredTxMetricContainer)

        enqueueingBlockSubscriber.subscribe(weBlockInfo(txList = mockTxList, height = Height(1)))

        em.flush()
        em.clear()

        val allEnqueuedAfterSecondInvocationTxs = enqueuedTxJpaRepository.findAll()
        assertEquals(mockTxList.size, allEnqueuedAfterSecondInvocationTxs.size)
        verify(exactly = 2) { txEnqueuedPredicate.isEnqueued(any()) }
        verify { totalTxMetricsContainer.add(2) }
        verify { filteredTxMetricContainer.add(0) }
    }

    @Test
    fun `should set default partition id from config`() {
        val first = TestDataFactory.policyDataHashTx(id = TxId.fromByteArray("2_1".toByteArray()))
        val second = TestDataFactory.createContractTx(id = TxId.fromByteArray("2_2".toByteArray()))
        val mockTxList: List<Tx> = listOf(first, second)
        every { txEnqueuedPredicate.isEnqueued(any()) } returns true
        every { txQueuePartitionResolver.resolvePartitionId(first) } returns "partFirstId"
        every { txQueuePartitionResolver.resolvePartitionId(second) } returns null

        enqueueingBlockSubscriber.subscribe(weBlockInfo(txList = mockTxList, height = Height(1)))

        em.flush()
        em.clear()

        txQueuePartitionJpaRepository.findAll().apply {
            assertEquals(2, size)
        }
        val defaultIdPartition = txQueuePartitionJpaRepository.findByIdOrNull("thatDefaultPartitionId")
        assertNotNull(defaultIdPartition)
        val enqueuedTxWithDefaultPartition = enqueuedTxJpaRepository.findByIdOrNull(second.id.asBase58String())
        assertNotNull(enqueuedTxWithDefaultPartition)
        assertEquals(defaultIdPartition, enqueuedTxWithDefaultPartition!!.partition)
    }

    @Test
    fun `should handle atomic with flat mapping it and setting position in atomic`() {
        val commonSender = Address(randomBytesFromUUID())
        val commonAtomicBadge = AtomicBadge(trustedSender = commonSender)
        val first = TestDataFactory.policyDataHashTx(
            id = TxId.fromByteArray("3_1".toByteArray()),
            atomicBadge = commonAtomicBadge,
        )
        val second = TestDataFactory.createContractTx(
            id = TxId.fromByteArray("3_2".toByteArray()),
            atomicBadge = commonAtomicBadge,
        )
        val third = TestDataFactory.callContractTx(
            id = TxId.fromByteArray("3_3".toByteArray()),
            atomicBadge = commonAtomicBadge,
        )
        val forth = TestDataFactory.callContractTx(
            id = TxId.fromByteArray("3_4".toByteArray()),
            atomicBadge = commonAtomicBadge,
        )
        val fifth = TestDataFactory.callContractTx(
            id = TxId.fromByteArray("3_5".toByteArray()),
            atomicBadge = commonAtomicBadge,
        )
        val atomic = TestDataFactory.atomicTx(
            id = TxId.fromByteArray("3_atomic1".toByteArray()),
            txs = listOf(first, second, third, forth, fifth),
            senderAddress = commonSender,
        )
        val sixth = TestDataFactory.callContractTx(
            id = TxId.fromByteArray("3_6".toByteArray()),
            atomicBadge = commonAtomicBadge,
        )

        every { txEnqueuedPredicate.isEnqueued(any()) } returns true
        val partFirstId = "part1"
        val partSecondId = "part2"
        val existingPart = TxQueuePartition(
            id = "existingPart3",
            priority = -1,
        )
        txQueuePartitionJpaRepository.saveAndFlush(existingPart)
        every { txQueuePartitionResolver.resolvePartitionId(first) } returns partFirstId
        every { txQueuePartitionResolver.resolvePartitionId(second) } returns partSecondId
        every { txQueuePartitionResolver.resolvePartitionId(third) } returns existingPart.id
        every { txQueuePartitionResolver.resolvePartitionId(forth) } returns partFirstId
        every { txQueuePartitionResolver.resolvePartitionId(fifth) } returns partFirstId
        every { txQueuePartitionResolver.resolvePartitionId(sixth) } returns partFirstId
        val height = 123L

        val mockTxList = listOf(atomic, sixth)
        enqueueingBlockSubscriber.subscribe(weBlockInfo(txList = mockTxList, height = Height(height)))

        em.flush()
        em.clear()

        val allEnqueuedTxs = enqueuedTxJpaRepository.findAll(Sort.by("positionInBlock"))
        val mockedTxIdSet = mutableSetOf(atomic.id, sixth.id)
        mockedTxIdSet += atomic.txs.map { it.id }.toSet()
        assertEquals(mockedTxIdSet.size, allEnqueuedTxs.size)
        val resultingTxIdSet = allEnqueuedTxs.map { TxId.fromBase58(it.id) }.toSet()
        assertEquals(allEnqueuedTxs.size, resultingTxIdSet.size)
        assertEquals(mockedTxIdSet, resultingTxIdSet)
        assertTrue(allEnqueuedTxs.all { it.status == EnqueuedTxStatus.NEW })
        assertTrue(allEnqueuedTxs.all { it.blockHeight == height })
        assertEquals(0, allEnqueuedTxs.first().positionInBlock)
        assertEquals(mockTxList.size - 1, allEnqueuedTxs.last().positionInBlock)
        assertEquals(
            mockTxList.first().timestamp.toDateTimeFromUTCBlockChain().nano,
            allEnqueuedTxs.first().txTimestamp.nano
        )
        val txById = allEnqueuedTxs.associateBy { TxId.fromBase58(it.id) }
        txById.apply {
            assertEquals(0, this[atomic.id]?.positionInAtomic)
            assertNull(this[atomic.id]?.atomicTxId)

            atomic.txs.withIndex().forEach { (index, tx) ->
                assertEquals(index + 1, this[tx.id]?.positionInAtomic)
                assertEquals(atomic.id.asBase58String(), this[tx.id]?.atomicTxId)
                assertEquals(this[atomic.id]?.positionInBlock, this[tx.id]?.positionInBlock)
            }

            assertNull(this[sixth.id]?.positionInAtomic)
            assertNull(this[sixth.id]?.atomicTxId)
//            assertEquals("thatDefaultPartitionId", this[atomic.id]?.partition?.id)
        }

        val mappedTx = allEnqueuedTxs.map { objectMapper.treeToValue(it.body, TxDto::class.java) }
        assertEquals(allEnqueuedTxs.size, mappedTx.size)
        verify(exactly = mockedTxIdSet.size) { txEnqueuedPredicate.isEnqueued(any()) }
        verify(exactly = mockedTxIdSet.size) { txQueuePartitionResolver.resolvePartitionId(any()) }

        val allPartitions = txQueuePartitionJpaRepository.findAll()
        assertEquals(4, allPartitions.size)

        verify { totalTxMetricsContainer.add(2) }
        verify { totalLogicalTxMetricsContainer.add(7) }
    }

    @Test
    fun `should do nothing when tx list is empty`() {
        enqueueingBlockSubscriber.subscribe(weBlockInfo(txList = emptyList(), height = Height(0)))

        verify(exactly = 0) { totalTxMetricsContainer.add(any()) }
    }

    private fun weBlockInfo(txList: List<Tx>, height: Height): WeBlockInfo = HttpApiWeBlockInfo(
        blockAtHeight(
            transactions = txList,
            transactionCount = txList.size.toLong(),
            height = height,
        )
    )
}
