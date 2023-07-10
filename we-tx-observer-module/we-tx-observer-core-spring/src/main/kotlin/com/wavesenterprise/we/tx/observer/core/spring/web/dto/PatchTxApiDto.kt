package com.wavesenterprise.we.tx.observer.core.spring.web.dto

import com.wavesenterprise.we.tx.observer.domain.EnqueuedTxStatus

data class PatchTxApiDto(
    val status: EnqueuedTxStatus,
)
