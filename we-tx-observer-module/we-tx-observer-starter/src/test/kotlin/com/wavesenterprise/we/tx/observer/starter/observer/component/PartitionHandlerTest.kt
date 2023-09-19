package com.wavesenterprise.we.tx.observer.starter.observer.component

import com.ninjasquad.springmockk.MockkBean
import com.wavesenterprise.we.flyway.starter.FlywaySchemaConfiguration
import com.wavesenterprise.we.tx.observer.core.spring.metrics.AddableLongMetricsContainer
import com.wavesenterprise.we.tx.observer.core.spring.partition.PartitionHandler
import com.wavesenterprise.we.tx.observer.jpa.TxObserverJpaAutoConfig
import com.wavesenterprise.we.tx.observer.jpa.config.TxObserverJpaConfig
import com.wavesenterprise.we.tx.observer.starter.TxObserverStarterConfig
import com.wavesenterprise.we.tx.observer.starter.observer.config.NodeBlockingServiceFactoryMockConfiguration
import com.wavesenterprise.we.tx.observer.starter.observer.config.ObjectMapperConfig
import io.mockk.verify
import io.mockk.verifySequence
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
        TxObserverJpaAutoConfig::class,
        NodeBlockingServiceFactoryMockConfiguration::class,
        TxObserverStarterConfig::class,
        FlywaySchemaConfiguration::class,
        TxObserverJpaConfig::class,
    ]
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
internal class PartitionHandlerTest {

    @Autowired
    lateinit var partitionHandler: PartitionHandler

    @MockkBean(name = "handledTxMetricsContainer", relaxed = true)
    lateinit var handledTxMetricsContainer: AddableLongMetricsContainer

    @MockkBean(name = "partitionFailureMetricsContainer", relaxed = true)
    lateinit var partitionFailureMetricsContainer: AddableLongMetricsContainer

    @MockkBean(name = "partitionHandlerJpa", relaxed = true)
    lateinit var partitionHandlerJpa: PartitionHandler

    @Test
    fun `should add metric and call partitionHandlerJpa on handleSuccessWhenReading`() {
        val partitionId = "cat"
        val txCount: Long = 5
        partitionHandler.handleSuccessWhenReading(partitionId, txCount)

        verify { handledTxMetricsContainer.add(txCount) }
        verify { partitionHandlerJpa.handleSuccessWhenReading(partitionId, txCount) }
        verify(exactly = 0) { partitionFailureMetricsContainer.add(any()) }
    }

    @Test
    fun `should add metric and call partitionHandlerJpa on handleErrorWhenReading`() {
        val partitionId = "st23"
        partitionHandler.handleErrorWhenReading(partitionId)

        verifySequence {
            partitionFailureMetricsContainer.add(1)
            partitionHandlerJpa.handleErrorWhenReading(partitionId)
        }
        verify(exactly = 0) { handledTxMetricsContainer.add(any()) }
    }

    @Test
    fun `should just delegate to partitionHandlerJpa on handleEmptyPartition`() {
        val partitionId = "fdksm"
        partitionHandler.handleEmptyPartition(partitionId)

        verify { partitionHandlerJpa.handleEmptyPartition(partitionId) }
        verify(exactly = 0) { handledTxMetricsContainer.add(any()) }
        verify(exactly = 0) { partitionFailureMetricsContainer.add(any()) }
    }
}
