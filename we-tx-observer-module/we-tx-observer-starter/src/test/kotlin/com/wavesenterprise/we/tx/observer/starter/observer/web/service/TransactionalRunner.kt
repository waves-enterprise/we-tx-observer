package com.wavesenterprise.we.tx.observer.starter.observer.web.service

import java.util.function.Consumer
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.transaction.Transactional
import javax.transaction.Transactional.TxType

open class TransactionalRunner {

    @PersistenceContext
    private lateinit var em: EntityManager

    @Transactional(TxType.REQUIRES_NEW)
    open fun doInTransaction(c: Consumer<EntityManager>) {
        c.accept(em)
    }
}
