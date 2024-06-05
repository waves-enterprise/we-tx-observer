package com.wavesenterprise.sdk.tx.observer.jpa.repository

import com.wavesenterprise.sdk.tx.observer.common.jpa.util.TX_OBSERVER_SCHEMA_NAME
import com.wavesenterprise.sdk.tx.observer.domain.BlockHistory
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.stream.Stream

@Repository
interface BlockHistoryRepository : CrudRepository<BlockHistory, String>, JpaSpecificationExecutor<BlockHistory> {
    @Query
    fun deleteAllByHeightBefore(height: Long)

    @Query
    fun deleteAllByHeightAfter(height: Long)

    @Query
    fun findAllByHeightBetweenOrderByHeightDesc(fromHeight: Long, toHeight: Long): Stream<BlockHistory>

    @Query(
        """
            delete from $TX_OBSERVER_SCHEMA_NAME.block_history
            where deleted = true
        """,
        nativeQuery = true
    )
    @Transactional
    @Modifying
    fun cleanDeleted(): Int
}
