package com.wavesenterprise.we.tx.observer.jpa.spectification.utils

import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root
import javax.persistence.metamodel.SingularAttribute

fun <R, T> Collection<T>.toIn(
    root: Root<R>,
    cb: CriteriaBuilder,
    column: SingularAttribute<R, T>
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
