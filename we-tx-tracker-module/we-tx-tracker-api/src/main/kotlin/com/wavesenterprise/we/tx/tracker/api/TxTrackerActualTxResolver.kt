package com.wavesenterprise.we.tx.tracker.api

import com.wavesenterprise.sdk.node.domain.tx.Tx

interface TxTrackerActualTxResolver {
    fun actualTx(tx: Tx): Tx
}
