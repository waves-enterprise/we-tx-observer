package com.wavesenterprise.we.tx.observer.starter.observer.executor

import com.ninjasquad.springmockk.SpykBean
import com.wavesenterprise.sdk.node.client.http.tx.CreateContractTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.we.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.we.tx.observer.api.BlockListenerException
import com.wavesenterprise.we.tx.observer.core.spring.partition.LatestTxPartitionPoller
import com.wavesenterprise.we.tx.observer.core.spring.partition.PollingTxSubscriber
import com.wavesenterprise.we.tx.observer.domain.TxQueuePartition
import com.wavesenterprise.we.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.we.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.we.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.we.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration
import com.wavesenterprise.we.tx.observer.starter.observer.config.ObjectMapperConfig
import com.wavesenterprise.we.tx.observer.starter.observer.util.ModelFactory.enqueuedTx
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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
internal class ErrorHandlingLatestTxPartitionPollerIntegrationTest {

    @Autowired
    lateinit var txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository

    @Autowired
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    @Autowired
    lateinit var errorHandlingLatestTxPartitionPoller: LatestTxPartitionPoller

    @SpykBean
    lateinit var pollingTxSubscriber: PollingTxSubscriber

    @PersistenceContext
    lateinit var em: EntityManager

    @ParameterizedTest
    @MethodSource("exceptions")
    fun `should decrement priority of a partition with error`(ex: Exception) {
        val firstErrorPartition = TxQueuePartition(
            id = "firstErrorPartitionId",
            priority = 0
        )
        val secondOkPartition = firstErrorPartition.copy(id = "secondOkPartitionId")
        val thirdErrorPartition = firstErrorPartition.copy(id = "thirdErrorPartitionId")
        val partitions = listOf(
            firstErrorPartition,
            secondOkPartition,
            thirdErrorPartition
        )
        txQueuePartitionJpaRepository.saveAll(
            partitions
        )
        partitions.map {
            enqueuedTx(
                tx = TestDataFactory.createContractTx().toDto(),
                partition = it
            )
        }.also { enqueuedTxJpaRepository.saveAll(it) }

        val errorPartitions = listOf(
            firstErrorPartition,
            thirdErrorPartition
        )
        errorPartitions.forEach {
            every { pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(partitionId = it.id) } throws ex
        }

        em.apply {
            flush()
            clear()
        }
        TestTransaction.flagForCommit()
        TestTransaction.end()

        repeat(3) { errorHandlingLatestTxPartitionPoller.pollLatestActualPartition() }

        verify {
            errorPartitions.forEach {
                pollingTxSubscriber.dequeuePartitionAndSendToSubscribers(partitionId = it.id)
            }
        }
        errorPartitions.forEach {
            txQueuePartitionJpaRepository.findByIdOrNull(it.id)!!.apply {
                assertEquals(-1, priority)
            }
        }
        txQueuePartitionJpaRepository.findByIdOrNull(secondOkPartition.id)!!.apply {
            assertEquals(0, priority)
        }
    }

    @AfterEach
    fun tearDown() {
        enqueuedTxJpaRepository.deleteAll()
        txQueuePartitionJpaRepository.deleteAll()
    }

    companion object {

        @JvmStatic
        fun exceptions() = setOf(
            BlockListenerException("bla bla", Exception()),
            IllegalArgumentException()
        ).map { Arguments.of(it) }.stream()
    }
}
