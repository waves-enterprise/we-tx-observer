package com.wavesenterprise.we.tx.observer.core.spring.lock

/**
 * Service for synchronisation using locks.
 */
interface LockService {

    /**
     * Lock by key
     * @param key
     * @return boolean
     */
    fun lock(key: String): Boolean
}
