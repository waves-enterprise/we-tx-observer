package com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.we.tx.observer.domain.BlockHistory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BlockHistoryService {
    fun saveBlocks(blocks: List<SyncedBlockInfo>)
    fun clean(observerHeight: Long)
    fun list(pageable: Pageable): Page<BlockHistory>
    fun latestCommonBlockWithNode(observerHeight: Height): BlockSearchResult
}
