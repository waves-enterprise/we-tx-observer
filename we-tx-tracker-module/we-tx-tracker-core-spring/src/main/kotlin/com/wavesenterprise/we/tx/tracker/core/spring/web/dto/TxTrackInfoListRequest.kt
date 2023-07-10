package com.wavesenterprise.we.tx.tracker.core.spring.web.dto

import com.wavesenterprise.we.tx.tracker.domain.TxTrackStatus
import java.util.UUID

data class TxTrackInfoListRequest(
    val status: List<TxTrackStatus>? = null,
    val userId: UUID? = null,
    val contractId: String? = null,
)
