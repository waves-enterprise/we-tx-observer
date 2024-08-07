package com.wavesenterprise.sdk.tx.observer.common.tx.subscriber

import com.wavesenterprise.sdk.node.domain.tx.Tx

interface TxSubscriber {
    fun subscribe(tx: Tx)
}
