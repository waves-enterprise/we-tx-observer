package com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature

interface SyncInfo {
    val nodeHeight: Height
    val observerHeight: Height
    val prevBlockSignature: Signature?
}
