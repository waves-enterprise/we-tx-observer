package com.wavesenterprise.we.tx.observer.component

import com.wavesenterprise.sdk.node.domain.tx.Tx

class SubscriberImpl(
    val predicate: (Tx) -> Boolean,
    val handlers: List<Handler>
) : Subscriber {
    override fun subscribe(tx: Tx) {
        if (predicate(tx)) {
            handlers.forEach { handler ->
                handler.handle(tx)
            }
        }
    }
}
