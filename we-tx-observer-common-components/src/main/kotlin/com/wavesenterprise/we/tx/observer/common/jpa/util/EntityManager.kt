package com.wavesenterprise.we.tx.observer.common.jpa.util

import javax.persistence.EntityManager

fun EntityManager.flushAndClear() {
    flush()
    clear()
}
