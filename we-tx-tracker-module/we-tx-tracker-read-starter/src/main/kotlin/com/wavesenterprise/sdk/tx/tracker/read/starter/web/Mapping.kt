package com.wavesenterprise.sdk.tx.tracker.read.starter.web

import com.wavesenterprise.sdk.tx.observer.common.jpa.util.toEq
import com.wavesenterprise.sdk.tx.observer.common.jpa.util.toIn
import com.wavesenterprise.sdk.tx.tracker.domain.SmartContractInfo_
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackInfo
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackInfo_
import com.wavesenterprise.sdk.tx.tracker.read.starter.web.dto.TxTrackInfoApiDto
import com.wavesenterprise.sdk.tx.tracker.read.starter.web.dto.TxTrackInfoListRequest
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.jpa.domain.Specification

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
