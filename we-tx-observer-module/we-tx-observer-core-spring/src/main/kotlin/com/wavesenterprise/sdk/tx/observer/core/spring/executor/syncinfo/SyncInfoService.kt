package com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo

interface SyncInfoService {
    fun observerHeight(): Long
    fun syncInfo(): SyncInfo
    fun syncInfoOnFork(): SyncInfo
    fun syncedTo(
        height: Long,
        prevBlockSignature: String? = null,
        syncedBlocks: List<SyncedBlockInfo> = emptyList(),
    )

    fun syncedTo(
        height: Long,
        prevBlockSignature: String? = null,
        expectedCurrentHeight: Long,
        syncedBlocks: List<SyncedBlockInfo> = emptyList(),
    )

    fun resetHeightIfNeeded()
}
