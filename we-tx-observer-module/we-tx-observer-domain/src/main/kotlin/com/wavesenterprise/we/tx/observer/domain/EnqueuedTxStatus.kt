package com.wavesenterprise.we.tx.observer.domain

enum class EnqueuedTxStatus {
    NEW,
    READ,
    POSTPONED,
    CANCELLED_FORKED,
    ;
}
