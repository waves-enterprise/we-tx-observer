package com.wavesenterprise.we.tx.tracker.read.starter.web

import com.wavesenterprise.we.tx.observer.common.annotation.toEq
import com.wavesenterprise.we.tx.observer.common.annotation.toIn
import com.wavesenterprise.we.tx.tracker.domain.SmartContractInfo_
import com.wavesenterprise.we.tx.tracker.domain.TxTrackInfo
import com.wavesenterprise.we.tx.tracker.domain.TxTrackInfo_
import com.wavesenterprise.we.tx.tracker.read.starter.web.dto.TxTrackInfoApiDto
import com.wavesenterprise.we.tx.tracker.read.starter.web.dto.TxTrackInfoListRequest
import org.springframework.data.jpa.domain.Specification
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root

fun TxTrackInfo.toApiDto() = TxTrackInfoApiDto(
    id = id,
    status = status,
    type = type,
    body = body,
    errors = errors,
    meta = meta,
    created = created,
    modified = modified,
    userId = userId,
    smartContractId = smartContractInfo?.id,
)

fun TxTrackInfoListRequest.toSpecification(): Specification<TxTrackInfo> =
    Specification { root: Root<TxTrackInfo>, cq: CriteriaQuery<*>, cb: CriteriaBuilder ->

        val predicates = mutableListOf<Predicate>()
        userId?.let {
            predicates += it.toEq(root, cb, TxTrackInfo_.userId)
        }
        contractId?.let {
            predicates += cb.equal(
                root.join(TxTrackInfo_.smartContractInfo).get(SmartContractInfo_.id),
                it
            )
        }
        status?.let {
            predicates += it.toIn(root, cb, TxTrackInfo_.status)
        }
        if (predicates.isNotEmpty()) {
            predicates.reduce { p1, p2 ->
                cb.and(p1, p2)
            }
        } else {
            null
        }
    }
