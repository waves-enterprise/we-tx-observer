package com.wavesenterprise.sdk.tx.tracker.api

import com.wavesenterprise.sdk.node.domain.tx.Tx

interface TxTrackerActualTxResolver {
    fun actualTx(tx: Tx): Tx
}
