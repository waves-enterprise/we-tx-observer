package com.wavesenterprise.we.tx.observer.jpa.repository

import com.wavesenterprise.we.tx.observer.common.jpa.util.TX_OBSERVER_SCHEMA_NAME
import com.wavesenterprise.we.tx.observer.domain.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.data.repository.Repository as SpringDataRepository

@Repository
interface LockRepository : SpringDataRepository<Lock, String> {
    @Query(
        """
        insert 
        into
            $TX_OBSERVER_SCHEMA_NAME.lock
            (name) 
        values
            (:name)
        on conflict do nothing
    """,
        nativeQuery = true
    )
    @Modifying
    fun create(name: String): Int

    @Query(
        """
        select * from $TX_OBSERVER_SCHEMA_NAME.lock
        where name = :name
        for update skip locked
    """,
        nativeQuery = true
    )
    fun acquire(name: String): Lock?
}
