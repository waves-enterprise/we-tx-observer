package com.wavesenterprise.we.tx.observer.starter.observer.executor

import com.ninjasquad.springmockk.MockkBean
import com.wavesenterprise.sdk.node.client.blocking.blocks.BlocksService
import com.wavesenterprise.sdk.node.client.blocking.tx.TxService
import com.wavesenterprise.sdk.node.client.http.tx.CallContractTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.sdk.node.test.data.TestDataFactory.Companion.txInfo
import com.wavesenterprise.sdk.node.test.data.Util
import com.wavesenterprise.we.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.we.tx.observer.core.spring.executor.ScheduledForkResolver
import com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo.SyncInfoService
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus.CANCELLED_FORKED
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus.NEW
import com.wavesenterprise.we.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.we.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.we.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration
import com.wavesenterprise.we.tx.observer.starter.observer.config.ObjectMapperConfig
import com.wavesenterprise.we.tx.observer.starter.observer.util.ModelFactory
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TestTransaction
import java.util.Optional

@DataJpaTest(
    properties = [
        "tx-observer.fork-resolver.window = 2",
        "tx-observer.fork-resolver.height-offset = 1000",
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
class ScheduledForkResolverTest {

    @Autowired
    lateinit var scheduledForkResolver: ScheduledForkResolver

    @Autowired
    lateinit var txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository

    @Autowired
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    @MockkBean(relaxed = true)
    lateinit var syncInfoService: SyncInfoService

    @MockkBean(relaxed = true)
    lateinit var txService: TxService

    @MockkBean(relaxed = true)
    lateinit var blocksService: BlocksService

    @BeforeEach
    fun setUp() {
        txQueuePartitionJpaRepository.saveAndFlush(NodeBlockingServiceFactoryMockConfiguration.mockPartition)
    }

    @AfterEach
    fun cleanup() {
        enqueuedTxJpaRepository.deleteAll()
        txQueuePartitionJpaRepository.deleteAll()
    }

    @Test
    fun `should skip fork resolving when height limit is more than node height`() {
        val txCount = 5
        every { syncInfoService.observerHeight() } returns 3001
        every { blocksService.blockHeight() } returns Height(100)
        every { txService.txInfo(any()) } returns Optional.empty()

        repeat(txCount) {
            enqueueTx(status = NEW, blockHeight = 2000)
        }
        TestTransaction.flagForCommit()
        TestTransaction.end()

        scheduledForkResolver.resolveForkedTx()

        enqueuedTxJpaRepository.findAll().apply {
            assertTrue(none { it.status == CANCELLED_FORKED })
            assertEquals(txCount, filter { it.status == NEW }.size)
        }
        verify(exactly = 0) { txService.txInfo(any()) }
    }

    @Test
    fun `should set status CANCELLED_FORKED when tx is not found in node`() {
        val txCount = 5
        val forkCheckWindowSize = 2
        every { syncInfoService.observerHeight() } returns 3001
        every { blocksService.blockHeight() } returns Height(50000)
        every { txService.txInfo(any()) } returns Optional.empty()

        repeat(txCount) {
            enqueueTx(status = NEW, blockHeight = 2000)
        }
        TestTransaction.flagForCommit()
        TestTransaction.end()

        scheduledForkResolver.resolveForkedTx()

        enqueuedTxJpaRepository.findAll().apply {
            assertEquals(txCount - forkCheckWindowSize, filter { it.status == NEW }.size)
            assertEquals(forkCheckWindowSize, filter { it.status == CANCELLED_FORKED }.size)
        }
    }

    @Test
    fun `should not change status when tx is found in node`() {
        val txCount = 5
        every { syncInfoService.observerHeight() } returns 3001
        every { blocksService.blockHeight() } returns Height(50000)
        every { txService.txInfo(any()) } returns Optional.of(txInfo())

        repeat(txCount) {
            enqueueTx(status = NEW, blockHeight = 2000)
        }

        TestTransaction.flagForCommit()
        TestTransaction.end()

        scheduledForkResolver.resolveForkedTx()

        enqueuedTxJpaRepository.findAll().apply {
            assertTrue(none { it.status == CANCELLED_FORKED })
            assertEquals(txCount, filter { it.status == NEW }.size)
        }
    }

    private fun enqueueTx(status: EnqueuedTxStatus, blockHeight: Long) {
        enqueuedTxJpaRepository.save(
            ModelFactory.enqueuedTx(
                tx = TestDataFactory.callContractTx(id = TxId.fromByteArray(Util.randomBytesFromUUID())).toDto(),
                status = status,
                partition = NodeBlockingServiceFactoryMockConfiguration.mockPartition,
                blockHeight = blockHeight
            )
        )
    }
}
