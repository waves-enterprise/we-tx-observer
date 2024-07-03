package com.wavesenterprise.sdk.tx.observer.core.spring.web

import com.wavesenterprise.sdk.tx.observer.common.jpa.util.toIn
import com.wavesenterprise.sdk.tx.observer.core.spring.web.dto.BlockHistoryApiDto
import com.wavesenterprise.sdk.tx.observer.core.spring.web.dto.EnqueuedTxApiDto
import com.wavesenterprise.sdk.tx.observer.core.spring.web.dto.EnqueuedTxSearchRequest
import com.wavesenterprise.sdk.tx.observer.core.spring.web.dto.PriorityComparisonOperator
import com.wavesenterprise.sdk.tx.observer.core.spring.web.dto.RollbackInfoApiDto
import com.wavesenterprise.sdk.tx.observer.core.spring.web.dto.TxQueuePartitionApiDto
import com.wavesenterprise.sdk.tx.observer.core.spring.web.dto.TxQueuePartitionSearchRequest
import com.wavesenterprise.sdk.tx.observer.domain.BlockHistory
import com.wavesenterprise.sdk.tx.observer.domain.EnqueuedTx
import com.wavesenterprise.sdk.tx.observer.domain.EnqueuedTxStatus
import com.wavesenterprise.sdk.tx.observer.domain.EnqueuedTx_
import com.wavesenterprise.sdk.tx.observer.domain.RollbackInfo
import com.wavesenterprise.sdk.tx.observer.domain.TxQueuePartition
import com.wavesenterprise.sdk.tx.observer.domain.TxQueuePartition_
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification

fun EnqueuedTx.toApiDto(): EnqueuedTxApiDto = EnqueuedTxApiDto(
    id = id,
    body = body,
    status = status,
    blockHeight = blockHeight,
    positionInBlock = positionInBlock,
    txTimestamp = txTimestamp,
    available = available,
    created = created,
    modified = modified,
)

fun EnqueuedTxSearchRequest.toSpecification() =
    Specification { root: Root<EnqueuedTx>,
        _: CriteriaQuery<*>,
        cb: CriteriaBuilder ->

        val predicates = mutableListOf<Predicate>()
        status?.let {
            predicates += it.toIn(root, cb, EnqueuedTx_.status)
        }
        blockHeight?.let {
            predicates += cb.equal(root.get(EnqueuedTx_.blockHeight), it)
        }
        partitionId?.let {
            predicates += cb.equal(
                root.join(EnqueuedTx_.partition).get(TxQueuePartition_.id),
                it
            )
        }
        available?.let {
            predicates += cb.equal(root.get(EnqueuedTx_.available), it)
        }
        txType?.let {
            predicates += it.toIn(root, cb, EnqueuedTx_.txType)
        }
        if (predicates.isNotEmpty()) {
            predicates.reduce { p1, p2 ->
                cb.and(p1, p2)
            }
        } else {
            null
        }
    }

fun TxQueuePartition.toApiDto(): TxQueuePartitionApiDto = TxQueuePartitionApiDto(
    id = id,
    priority = priority,
    pausedOnTxId = pausedOnTxId,
    created = created,
    modified = modified,
)

fun TxQueuePartitionSearchRequest.toSpecification() =
    Specification { root: Root<TxQueuePartition>,
        cq: CriteriaQuery<*>,
        cb: CriteriaBuilder ->

        val predicates = mutableListOf<Predicate>()
        priority?.let {
            predicates += priorityOp?.toPredicate(root, cb, it)
                ?: cb.equal(root.get(TxQueuePartition_.priority), priority)
        }
        status?.let {
            val subQuery = cq.subquery(EnqueuedTx::class.java)
            val subRoot = subQuery.from(EnqueuedTx::class.java)

            val joinPredicate = cb.equal(subRoot.get(EnqueuedTx_.partition), root)
            val firstTxPredicate = cb.equal(subRoot.get(EnqueuedTx_.positionInBlock), 0)
            val subQueryPredicate = it.toIn(subRoot, cb, EnqueuedTx_.status)

            subQuery.select(subRoot).where(joinPredicate, firstTxPredicate, subQueryPredicate)
            predicates += cb.exists(subQuery)
        }
        active?.let {
            val subQuery = cq.subquery(EnqueuedTx::class.java)
            val subRoot = subQuery.from(EnqueuedTx::class.java)
            val joinPredicate = cb.equal(subRoot.get(EnqueuedTx_.partition), root)

            subQuery.select(subRoot).where(
                joinPredicate,
                cb.equal(subRoot.get(EnqueuedTx_.status), EnqueuedTxStatus.NEW)
            )
            val newEnqueuedTxExist = cb.exists(subQuery)
            predicates += if (it) {
                newEnqueuedTxExist
            } else {
                cb.not(newEnqueuedTxExist)
            }
        }
        if (predicates.isNotEmpty()) {
            predicates.reduce { p1, p2 ->
                cb.and(p1, p2)
            }
        } else {
            null
        }
    }

fun PriorityComparisonOperator.toPredicate(root: Root<TxQueuePartition>, cb: CriteriaBuilder, priority: Int): Predicate {
    return when (this) {
        PriorityComparisonOperator.EQ -> cb.equal(root.get(TxQueuePartition_.priority), priority)
        PriorityComparisonOperator.NE -> cb.notEqual(root.get(TxQueuePartition_.priority), priority)
        PriorityComparisonOperator.LT -> cb.lessThan(root.get(TxQueuePartition_.priority), priority)
        PriorityComparisonOperator.LE -> cb.le(root.get(TxQueuePartition_.priority), priority)
        PriorityComparisonOperator.GT -> cb.greaterThan(root.get(TxQueuePartition_.priority), priority)
        PriorityComparisonOperator.GE -> cb.ge(root.get(TxQueuePartition_.priority), priority)
    }
}

fun RollbackInfo.toApiDto(): RollbackInfoApiDto =
    RollbackInfoApiDto(
        toHeight = toHeight,
        toBlockSignature = toBlockSignature,
        datetime = createdTimestamp,
    )

fun BlockHistory.toApiDto(): BlockHistoryApiDto =
    BlockHistoryApiDto(
        signature = signature,
        height = height,
        timestamp = timestamp,
        createdTimestamp = createdTimestamp,
    )
