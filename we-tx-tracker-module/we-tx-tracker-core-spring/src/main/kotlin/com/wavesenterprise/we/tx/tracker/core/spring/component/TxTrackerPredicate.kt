package com.wavesenterprise.we.tx.tracker.core.spring.component

import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.we.tx.tracker.api.TxTracker
import com.wavesenterprise.we.tx.tracker.api.TxTrackerActualTxResolver

class TxTrackerPredicate(
    private val txTrackerActualTxResolver: TxTrackerActualTxResolver,
    private val txTracker: TxTracker,
) : (Tx) -> Boolean {
    override fun invoke(tx: Tx): Boolean =
        txTrackerActualTxResolver.actualTx(tx)
            .let(txTracker::existsInTracker)
}
