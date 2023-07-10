package com.wavesenterprise.we.tx.observer.starter.lock

import com.wavesenterprise.we.tx.observer.common.tx.executor.TxExecutor
import com.wavesenterprise.we.tx.observer.core.spring.lock.LockService
import com.wavesenterprise.we.tx.observer.core.spring.lock.LockServiceImpl
import com.wavesenterprise.we.tx.observer.jpa.repository.LockRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LockConfig {

    @Autowired
    lateinit var lockRepository: LockRepository

    @Autowired
    lateinit var txExecutor: TxExecutor

    @Bean
    fun lockService(
        lockRepository: LockRepository,
        txExecutor: TxExecutor,
    ): LockService =
        LockServiceImpl(
            lockRepository = lockRepository,
            txExecutor = txExecutor,
        )
}
