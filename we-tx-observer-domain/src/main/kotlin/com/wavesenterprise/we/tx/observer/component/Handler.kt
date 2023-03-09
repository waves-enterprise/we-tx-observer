package com.wavesenterprise.we.tx.observer.component

import com.wavesenterprise.sdk.node.domain.tx.Tx

interface Handler {
    fun handle(tx: Tx)
}
