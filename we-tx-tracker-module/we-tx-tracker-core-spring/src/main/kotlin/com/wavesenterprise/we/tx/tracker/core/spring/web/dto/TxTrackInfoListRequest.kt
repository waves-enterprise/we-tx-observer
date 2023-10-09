package com.wavesenterprise.we.tx.tracker.core.spring.web.dto

import com.wavesenterprise.we.tx.tracker.domain.TxTrackStatus

data class TxTrackInfoListRequest(
    val status: List<TxTrackStatus>? = null,
    val userId: String? = null,
    val contractId: String? = null,
)
