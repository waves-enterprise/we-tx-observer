package com.wavesenterprise.sdk.tx.observer.common.jpa.util

import javax.persistence.EntityManager

fun EntityManager.flushAndClear() {
    flush()
    clear()
}
