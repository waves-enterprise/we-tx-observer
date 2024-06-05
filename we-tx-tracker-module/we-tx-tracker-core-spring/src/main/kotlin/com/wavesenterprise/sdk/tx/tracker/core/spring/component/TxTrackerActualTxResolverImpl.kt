package com.wavesenterprise.sdk.tx.tracker.core.spring.component

import com.wavesenterprise.sdk.node.domain.tx.ExecutedContractTx
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.tx.tracker.api.TxTrackerActualTxResolver

object TxTrackerActualTxResolverImpl : TxTrackerActualTxResolver {
    override fun actualTx(tx: Tx) = when (tx) {
        is ExecutedContractTx -> tx.tx
        else -> tx
    }
}
