package com.wavesenterprise.we.tx.observer.starter.observer.web.service

import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.function.Consumer
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

open class TransactionalRunner {

    @PersistenceContext
    private lateinit var em: EntityManager

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun doInTransaction(c: Consumer<EntityManager>) {
        c.accept(em)
    }
}
