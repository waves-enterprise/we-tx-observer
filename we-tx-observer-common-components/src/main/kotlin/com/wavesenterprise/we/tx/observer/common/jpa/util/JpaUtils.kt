package com.wavesenterprise.we.tx.observer.common.jpa.util

import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root
import javax.persistence.metamodel.SingularAttribute

const val TX_OBSERVER_SCHEMA_NAME = "tx_observer"
const val TX_TRACKER_SCHEMA_NAME = "tx_tracker"
const val JSONB_TYPE: String = "jsonb"

fun <R, T> Collection<T>.toIn(
    root: Root<R>,
    cb: CriteriaBuilder,
    column: SingularAttribute<R, T>,
): Predicate {
    val `in` = cb.`in`(root.get<T>(column))
    forEach {
        `in`.value(it)
    }
    return `in`.andNotNull(root, cb, column)
}

fun <R> Predicate.andNotNull(root: Root<R>, cb: CriteriaBuilder, column: SingularAttribute<R, *>) =
    cb.and(cb.isNotNull(root.get(column)), this)!!

fun <R, T : Comparable<T>> T.toEq(root: Root<R>, cb: CriteriaBuilder, column: SingularAttribute<R, T>): Predicate {
    return cb.equal(root.get(column), this).andNotNull(root, cb, column)
}
