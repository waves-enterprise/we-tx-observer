package com.wavesenterprise.sdk.tx.observer.domain

enum class EnqueuedTxStatus {
    NEW,
    READ,
    POSTPONED,
    CANCELLED_FORKED,
    ;
}
