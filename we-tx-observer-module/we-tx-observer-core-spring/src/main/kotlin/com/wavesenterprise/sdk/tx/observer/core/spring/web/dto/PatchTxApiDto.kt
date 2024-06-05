package com.wavesenterprise.sdk.tx.observer.core.spring.web.dto

import com.wavesenterprise.sdk.tx.observer.domain.EnqueuedTxStatus

data class PatchTxApiDto(
    val status: EnqueuedTxStatus,
)
