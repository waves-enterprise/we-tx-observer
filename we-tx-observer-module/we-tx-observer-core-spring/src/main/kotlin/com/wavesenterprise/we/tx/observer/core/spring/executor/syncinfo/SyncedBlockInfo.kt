package com.wavesenterprise.we.tx.observer.core.spring.executor.syncinfo

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.Timestamp

interface SyncedBlockInfo {
    val signature: Signature
    val height: Height
    val timestamp: Timestamp
}
