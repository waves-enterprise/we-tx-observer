package com.wavesenterprise.we.tx.observer.starter.observer.config

import com.wavesenterprise.we.tx.observer.common.tx.executor.TxExecutor
import com.wavesenterprise.we.tx.observer.domain.BlockHeightInfo
import com.wavesenterprise.we.tx.observer.jpa.repository.BlockHeightJpaRepository
import com.wavesenterprise.we.tx.observer.jpa.repository.BlockHistoryRepository
import com.wavesenterprise.we.tx.observer.jpa.repository.EnqueuedTxJpaRepository
import com.wavesenterprise.we.tx.observer.jpa.repository.LockRepository
import com.wavesenterprise.we.tx.observer.jpa.repository.RollbackInfoRepository
import com.wavesenterprise.we.tx.observer.starter.observer.util.TxExecutorStub
import io.mockk.every
import io.mockk.mockk
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(NodeBlockingServiceFactoryMockConfiguration::class)
class BlockListenerTestContextConfiguration {

    @Bean
    fun blockHeightJpaRepositoryMock(): BlockHeightJpaRepository = mockk {
        every { findAll() } returns listOf(
            BlockHeightInfo(
                currentHeight = 1L,
                prevBlockSignature = null,
            )
        )
    }

    @Bean
    fun jpaEnqueuedTxJpaRepository() = mockk<EnqueuedTxJpaRepository>()

    @Bean
    fun txExecutorStub(): TxExecutor = TxExecutorStub

    @Bean
    fun lockRepositoryMock(): LockRepository = mockk()

    @Bean
    fun rollbackInfoRepositoryMock(): RollbackInfoRepository = mockk()

    @Bean
    fun blockHistoryRepositoryMock(): BlockHistoryRepository = mockk()
}
