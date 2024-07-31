package com.wavesenterprise.sdk.tx.observer.starter.observer.web.service

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.function.Consumer

open class TransactionalRunner {

    @PersistenceContext
    private lateinit var em: EntityManager

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    open fun doInTransaction(c: Consumer<EntityManager>) {
        c.accept(em)
    }
}
