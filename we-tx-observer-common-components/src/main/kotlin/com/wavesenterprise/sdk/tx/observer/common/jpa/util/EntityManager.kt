package com.wavesenterprise.sdk.tx.observer.common.jpa.util

import jakarta.persistence.EntityManager

fun EntityManager.flushAndClear() {
    flush()
    clear()
}
