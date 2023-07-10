package com.wavesenterprise.we.tx.observer.core.spring.web.dto

data class QueueStatusApiDto(
    val nodeHeight: Long,
    val observerHeight: Long,
    val queueHeight: Long?,
    val queueSize: Long,
    val privacyStatusApiDto: PrivacyStatusApiDto,
)
