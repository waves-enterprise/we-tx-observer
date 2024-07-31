package com.wavesenterprise.sdk.tx.observer.jpa.repository

import com.wavesenterprise.sdk.tx.observer.common.jpa.util.TX_OBSERVER_SCHEMA_NAME
import com.wavesenterprise.sdk.tx.observer.domain.EnqueuedTx
import com.wavesenterprise.sdk.tx.observer.domain.EnqueuedTxStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Suppress("TooManyFunctions")
@Repository
interface EnqueuedTxJpaRepository : JpaRepository<EnqueuedTx, String>, JpaSpecificationExecutor<EnqueuedTx> {

    @Query(
        value = """
            select count(*) from $TX_OBSERVER_SCHEMA_NAME.enqueued_tx tx where tx.status = :#{#enqueuedTxStatus.name()}
        """,
        nativeQuery = true,
    )
    fun countByStatus(enqueuedTxStatus: EnqueuedTxStatus): Long

    @Query("select count(tx) from EnqueuedTx tx where status = 'NEW' and txType = 114")
    fun countPolicyDataHashes(): Long

    @Query("select count(tx) from EnqueuedTx tx where status = 'NEW' and txType = 114 and available = false")
    fun countNotAvailablePolicyDataHashes(): Long

    @Query("select tx from EnqueuedTx tx order by tx.blockHeight, tx.positionInBlock")
    fun findAllSorted(specification: Specification<EnqueuedTx>, pageable: Pageable): Page<EnqueuedTx>

    @Query(
        value = """
            select tx.* from $TX_OBSERVER_SCHEMA_NAME.enqueued_tx tx
            where tx.status = :#{#enqueuedTxStatus.name()} and tx.partition_id = :partitionId
            order by tx.block_height, tx.position_in_block, tx.position_in_atomic for update skip locked
        """,
        nativeQuery = true,
    )
    fun findActualEnqueuedTxForPartition(
        enqueuedTxStatus: EnqueuedTxStatus,
        partitionId: String,
        pageable: Pageable,
    ): Page<EnqueuedTx>

    fun findAllByStatusAndBlockHeightBeforeOrderByBlockHeight(
        enqueuedTxStatus: EnqueuedTxStatus,
        blockHeight: Long,
        pageable: Pageable,
    ): Page<EnqueuedTx>

    @Query(
        value = """
            select tx.* from $TX_OBSERVER_SCHEMA_NAME.enqueued_tx tx 
            where tx.status = 'NEW' 
                and tx.tx_type = 114
                and not tx.available
                and tx.tx_timestamp >=
                (
                    select tx.tx_timestamp
                    from $TX_OBSERVER_SCHEMA_NAME.enqueued_tx tx
                    where tx.status = 'NEW'
                      and tx.tx_type = 114
                      and not tx.available
                    order by tx.tx_timestamp
                    offset :offset limit 1    
                )
            order by tx.tx_timestamp
            limit :limit for update skip locked
    """,
        nativeQuery = true,
    )
    fun findOldCheckPrivacyAvailabilityCandidates(
        offset: Int,
        limit: Int,
    ): List<EnqueuedTx>

    @Query(
        value = """
            select tx.* from $TX_OBSERVER_SCHEMA_NAME.enqueued_tx tx 
            where tx.status = 'NEW' 
                and tx.tx_type = 114
                and not tx.available 
                and tx.tx_timestamp <=
                (
                    select tx.tx_timestamp
                    from $TX_OBSERVER_SCHEMA_NAME.enqueued_tx tx
                    where tx.status = 'NEW'
                      and tx.tx_type = 114
                      and not tx.available
                      and not tx.id in :alreadySelectedIds
                    order by tx.tx_timestamp desc
                    offset :offset limit 1    
                )
            order by tx.tx_timestamp desc
            offset :offset limit :limit for update skip locked
    """,
        nativeQuery = true,
    )
    fun findRecentCheckPrivacyAvailabilityCandidates(
        alreadySelectedIds: Set<String>,
        offset: Int,
        limit: Int,
    ): List<EnqueuedTx>

    @Query("select min(tx.blockHeight) from EnqueuedTx tx where tx.status = :enqueuedTxStatus")
    fun findMinHeightForStatus(enqueuedTxStatus: EnqueuedTxStatus): Long?

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query(
        """
            delete from $TX_OBSERVER_SCHEMA_NAME.enqueued_tx 
            where id in (
                select id 
                from $TX_OBSERVER_SCHEMA_NAME.enqueued_tx 
                where status = :enqueuedTxStatus 
                and block_height < :blockHeight limit :limit
            )
        """,
        nativeQuery = true,
    )
    @Modifying
    fun deleteAllReadWithBlockHeightBefore(
        enqueuedTxStatus: String = EnqueuedTxStatus.READ.name,
        blockHeight: Long,
        limit: Long,
    ): Int

    @Query(
        """delete from $TX_OBSERVER_SCHEMA_NAME.enqueued_tx where block_height >= :blockHeight""",
        nativeQuery = true,
    )
    @Modifying
    fun cleanAllWithBlockHeightMoreThan(blockHeight: Long): Int

    @Modifying
    fun deleteByStatus(enqueuedTxStatus: EnqueuedTxStatus): Int

    @Query(
        """update EnqueuedTx set status = :newStatus where status = :oldStatus
        and partition.id in (select p.id from TxQueuePartition p where p.priority < :priority)""",
    )
    @Modifying
    fun setStatusForTxWithStatusEqualsAndPartitionPriorityLowerThan(
        newStatus: EnqueuedTxStatus,
        oldStatus: EnqueuedTxStatus,
        priority: Int,
    ): Int

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select etx from EnqueuedTx etx where etx.id = :id")
    fun findByIdWithLock(id: String): EnqueuedTx?

    @Query("select e.id from EnqueuedTx e where id in :ids")
    fun existentTxIds(ids: Set<String>): Set<String>
}
