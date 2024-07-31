package com.wavesenterprise.sdk.tx.observer.core.spring.lock

import com.wavesenterprise.sdk.tx.observer.common.tx.executor.TxExecutor
import com.wavesenterprise.sdk.tx.observer.jpa.repository.LockRepository
import org.slf4j.debug
import org.slf4j.info
import org.slf4j.lazyLogger

class LockServiceImpl(
    private val lockRepository: LockRepository,
    private val txExecutor: TxExecutor,
) : LockService {
    private val log by lazyLogger(LockServiceImpl::class)

    override fun lock(key: String): Boolean {
        log.debug { "Trying to create row for lock: $key" }
        txExecutor.requiresNew {
            lockRepository.create(key).also { insertedCount ->
                if (insertedCount > 0) {
                    log.info { "Lock row created for key: $key" }
                } else {
                    log.debug { "Lock row already exists for key: $key" }
                }
            }
        }
        log.debug { "Trying to acquire lock for key: $key" }
        return (lockRepository.acquire(key) != null).also { locked ->
            log.info {
                "Lock ${if (!locked) "not " else ""}acquired for key: $key"
            }
        }
    }
}
