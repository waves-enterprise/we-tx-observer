package com.wavesenterprise.we.tx.observer.core.spring.lock

interface LockService {
    fun lock(key: String): Boolean
}
