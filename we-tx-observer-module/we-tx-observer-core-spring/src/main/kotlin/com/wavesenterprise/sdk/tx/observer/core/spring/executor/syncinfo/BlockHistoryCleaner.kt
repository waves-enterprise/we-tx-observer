package com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo

import com.wavesenterprise.sdk.tx.observer.jpa.repository.BlockHistoryRepository
import org.slf4j.info
import org.slf4j.lazyLogger

class BlockHistoryCleaner(
    private val blockHistoryRepository: BlockHistoryRepository,
) {
    private val log by lazyLogger(BlockHistoryCleaner::class)

    fun clean() {
        blockHistoryRepository.cleanDeleted().also { count ->
            log.info {
                "Cleaned $count rows"
            }
        }
    }
}
