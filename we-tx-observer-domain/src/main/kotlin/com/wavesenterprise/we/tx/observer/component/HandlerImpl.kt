package com.wavesenterprise.we.tx.observer.component

import com.wavesenterprise.sdk.node.domain.tx.Tx

class HandlerImpl : Handler {
    override fun handle(tx: Tx) {
        println(tx.toString()) // TODO if needed or remove
    }
}
