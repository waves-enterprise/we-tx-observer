package com.wavesenterprise.we.tx.observer.common.tx.handler

import com.wavesenterprise.sdk.node.domain.tx.Tx

interface TxHandler {
    fun handle(tx: Tx)
}
