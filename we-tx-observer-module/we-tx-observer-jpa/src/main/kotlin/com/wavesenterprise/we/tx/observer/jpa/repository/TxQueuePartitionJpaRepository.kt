package com.wavesenterprise.we.tx.observer.jpa.repository

import com.wavesenterprise.we.tx.observer.common.jpa.util.TX_OBSERVER_SCHEMA_NAME
import com.wavesenterprise.we.tx.observer.domain.TxQueuePartition
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
interface TxQueuePartitionJpaRepository :
    JpaRepository<TxQueuePartition, String>,
    JpaSpecificationExecutor<TxQueuePartition> {

    @Query(
        """
            select tqp.* from $TX_OBSERVER_SCHEMA_NAME.tx_queue_partition tqp
            where tqp.id = :id
            for update
        """,
        nativeQuery = true,
    )
    fun findAndLockById(id: String): TxQueuePartition?

    @Query(
        """
            select tqp.id from $TX_OBSERVER_SCHEMA_NAME.tx_queue_partition tqp
                join $TX_OBSERVER_SCHEMA_NAME.enqueued_tx etx on etx.partition_id = tqp.id and etx.status = 'NEW'
                    where tqp.paused_on_tx_id is null
                order by tqp.priority desc, etx.tx_timestamp
                for update of tqp skip locked
                limit 1
        """,
        nativeQuery = true
    )
    fun findAndLockLatestPartition(): String?

    @Query(
        """
            select tqp.id
            from $TX_OBSERVER_SCHEMA_NAME.tx_queue_partition tqp
            where tqp.paused_on_tx_id is null
            and exists(
                select * from $TX_OBSERVER_SCHEMA_NAME.enqueued_tx
                where partition_id = tqp.id
                and status = 'NEW'
            )
            order by tqp.priority desc
            for update of tqp skip locked
            limit 1
        """,
        nativeQuery = true,
    )
    fun findAndLockRandomPartition(): String?

    @Query(
        """
            select tqp.* from $TX_OBSERVER_SCHEMA_NAME.tx_queue_partition tqp
                join $TX_OBSERVER_SCHEMA_NAME.enqueued_tx etx on etx.partition_id = tqp.id and etx.status = 'NEW'
                    where tqp.paused_on_tx_id is null
                order by tqp.priority desc, etx.tx_timestamp
        """,
        nativeQuery = true
    )
    fun findActualPartitions(): List<TxQueuePartition>

    @Query(
        """
            update $TX_OBSERVER_SCHEMA_NAME.tx_queue_partition
                    set priority = 0
                where id = :partitionId
        """,
        nativeQuery = true
    )
    @Modifying
    fun updateSuccessTxHandle(partitionId: String)

    @Query(
        """
            update $TX_OBSERVER_SCHEMA_NAME.tx_queue_partition
                    set paused_on_tx_id = :pausedOnTxId
                where id = :partitionId and not 
                (select available from $TX_OBSERVER_SCHEMA_NAME.enqueued_tx where id = :pausedOnTxId)
        """,
        nativeQuery = true
    )
    @Modifying
    fun updatePausedTxId(partitionId: String, pausedOnTxId: String): Int

    @Query(
        """
            update $TX_OBSERVER_SCHEMA_NAME.tx_queue_partition
                    set paused_on_tx_id = null
                where id = :partitionId and paused_on_tx_id = :pausedOnTxId
        """,
        nativeQuery = true
    )
    @Modifying
    fun resetPausedTxId(partitionId: String, pausedOnTxId: String): Int

    @Query(
        value = """
            update $TX_OBSERVER_SCHEMA_NAME.tx_queue_partition
                    set priority = (priority - 1)
                where id = :partitionId
        """,
        nativeQuery = true
    )
    @Modifying
    fun updateErrorTxHandle(partitionId: String)

    @Query(
        """
            select count(p.id) from $TX_OBSERVER_SCHEMA_NAME.tx_queue_partition p 
                where p.priority < 0 
                and exists(select 1 from $TX_OBSERVER_SCHEMA_NAME.enqueued_tx etx
                               where etx.status = 'NEW'
                               and etx.partition_id = p.id) 
                and p.paused_on_tx_id is null;
        """,
        nativeQuery = true
    )
    fun countErrorPartitions(): Long

    @Query(
        """
            select count(p.id) from $TX_OBSERVER_SCHEMA_NAME.tx_queue_partition p
                where p.priority < $STUCK_PARTITION_PRIORITY_THRESHOLD 
                and exists(select 1 from $TX_OBSERVER_SCHEMA_NAME.enqueued_tx etx
                               where etx.status = 'NEW'
                               and etx.partition_id = p.id)
        """,
        nativeQuery = true
    )
    fun countStuckPartitions(): Long

    @Query(
        """
            update TxQueuePartition p set p.pausedOnTxId = null where p.pausedOnTxId is not null
            and not exists (select etx.id from EnqueuedTx etx
                               where etx.status = 'NEW'
                               and etx.id = p.pausedOnTxId)
        """
    )
    @Modifying
    fun clearPausedOnTxIds(): Int

    @Query(
        """
            with empty_partitions_batch 
            as (
                select tqp.id
                from tx_observer.tx_queue_partition tqp
                where not exists(
                    select *
                    from tx_observer.enqueued_tx etx
                    where etx.partition_id = tqp.id
                )
                limit :limit for update of tqp skip locked
            )
            delete
            from tx_observer.tx_queue_partition tqp
            using empty_partitions_batch where tqp.id = empty_partitions_batch.id;  
        """,
        nativeQuery = true,
    )
    @Modifying
    @Transactional
    fun deleteEmptyPartitions(limit: Int): Int
}

const val STUCK_PARTITION_PRIORITY_THRESHOLD: Int = -100
