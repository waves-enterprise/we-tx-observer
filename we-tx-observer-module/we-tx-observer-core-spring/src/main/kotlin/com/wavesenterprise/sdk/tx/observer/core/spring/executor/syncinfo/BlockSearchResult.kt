package com.wavesenterprise.sdk.tx.observer.core.spring.executor.syncinfo

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature

sealed class BlockSearchResult {
    data class Found(
        val signature: Signature,
        val height: Height,
    ) : BlockSearchResult()

    data class NotFound(
        val lastCheckedHeight: Height,
    ) : BlockSearchResult()
}
