package com.wavesenterprise.we.tx.observer.starter.observer.cache

import com.ninjasquad.springmockk.MockkBean
import com.wavesenterprise.we.tx.observer.core.spring.partition.TxPartitionPollerAccelerationHelper
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.we.tx.observer.jpa.repository.TxQueuePartitionJpaRepository
import com.wavesenterprise.we.tx.observer.starter.PartitionPollerConfig
import com.wavesenterprise.we.tx.observer.starter.cache.DefaultTimeBasedCacheConfiguration
import com.wavesenterprise.we.tx.observer.starter.properties.PartitionPollerProperties
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(
    classes = [
        DefaultTimeBasedCacheConfiguration::class,
        PartitionPollerConfig::class
    ]
)
@SpringBootTest
@Profile("test")
class DefaultTxPartitionPollerAccelerationHelperTest {

    @MockkBean(relaxed = true)
    private lateinit var txQueuePartitionJpaRepository: TxQueuePartitionJpaRepository

    @MockkBean
    private lateinit var enqueuedTxJpaRepository: EnqueuedTxJpaRepository

    @MockkBean
    private lateinit var partitionPollerProperties: PartitionPollerProperties

    @Autowired
    private lateinit var txPartitionPollerAccelerationHelper: TxPartitionPollerAccelerationHelper

    @Test
    fun `should use cache of query`() {
        val newEnqueuedTxCount = 100L
        every { enqueuedTxJpaRepository.countByStatus(EnqueuedTxStatus.NEW) } returns newEnqueuedTxCount
        every { partitionPollerProperties.accelerateAtQueueSize } returns newEnqueuedTxCount

        val firstResult = txPartitionPollerAccelerationHelper.isAccelerationRequired()
        assertEquals(firstResult, txPartitionPollerAccelerationHelper.isAccelerationRequired())
        verify(exactly = 1) { enqueuedTxJpaRepository.countByStatus(EnqueuedTxStatus.NEW) }
    }
}
