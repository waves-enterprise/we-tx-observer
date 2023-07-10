package com.wavesenterprise.we.tx.observer.starter.observer.jpa

import com.wavesenterprise.sdk.node.client.http.tx.CreateContractTxDto.Companion.toDto
import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.test.data.TestDataFactory
import com.wavesenterprise.we.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.we.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.we.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration.Companion.mockPartition
import com.wavesenterprise.we.tx.observer.starter.observer.util.ModelFactory.enqueuedTx
import com.wavesplatform.we.flyway.schema.starter.FlywaySchemaConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.transaction.TestTransaction

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(
    classes = [
        DataSourceAutoConfiguration::class,
        TxObserverJpaAutoConfig::class,
        TxObserverJpaConfig::class,
        NodeBlockingServiceFactoryMockConfiguration::class,
        FlywaySchemaConfiguration::class,
        TxObserverJpaConfig::class,
    ]
)
@AutoConfigureTestDatabase(replace = Replace.NONE)
internal class EnqueuedTxJpaRepositoryTest {

    @Autowired
    lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    @Autowired
    lateinit var txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository

    @BeforeEach
    fun setUp() {
        txQueuePartitionJpaRepository.saveAndFlush(mockPartition)
    }

    @Test
    fun `should delete all with blockHeightMoreThan100`() {
        // ARRANGE
        val oldHeight = 100L
        val txCountOnOldHeight = 4
        repeat(txCountOnOldHeight) {
            enqueuedTxJpaRepository.save(
                enqueuedTx(
                    tx = TestDataFactory.createContractTx(id = TxId.fromByteArray("id_old_$it".toByteArray())).toDto(),
                    positionInBlock = it,
                    blockHeight = oldHeight,
                    partition = mockPartition
                )
            )
        }
        val newHeight = oldHeight + 1
        val txCountOnNewHeight = 5
        repeat(txCountOnNewHeight) {
            enqueuedTxJpaRepository.save(
                enqueuedTx(
                    tx = TestDataFactory.createContractTx(id = TxId.fromByteArray("id_new_$it".toByteArray())).toDto(),
                    positionInBlock = it,
                    blockHeight = newHeight,
                    partition = mockPartition
                )
            )
        }
        TestTransaction.flagForCommit()

        // ACT
        val removedTxCount = enqueuedTxJpaRepository.deleteAllWithBlockHeightMoreThan(newHeight)
        TestTransaction.flagForCommit()
        TestTransaction.end()

        // ASSERT
        assertEquals(txCountOnOldHeight, enqueuedTxJpaRepository.findAll().count())
        assertEquals(txCountOnNewHeight, removedTxCount)
    }

    @AfterEach
    fun cleanup() {
        enqueuedTxJpaRepository.deleteAll()
        txQueuePartitionJpaRepository.deleteAll()
    }
}
