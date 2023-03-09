package com.wavesenterprise.we.tx.observer.api.jpa.model

enum class EnqueuedTxStatus {
    NEW,
    READ,
    POSTPONED,
    CANCELLED_FORKED
}
