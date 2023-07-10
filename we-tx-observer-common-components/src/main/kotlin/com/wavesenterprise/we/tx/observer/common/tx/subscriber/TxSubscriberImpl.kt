package com.wavesenterprise.we.tx.observer.common.tx.subscriber

import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.we.tx.observer.common.tx.handler.TxHandler

class TxSubscriberImpl(
    val predicate: (Tx) -> Boolean,
    val txHandlers: List<TxHandler>,
) : TxSubscriber {
    override fun subscribe(tx: Tx) {
        if (predicate(tx)) {
            txHandlers.forEach { handler ->
                handler.handle(tx)
            }
        }
    }
}
