package com.wavesenterprise.sdk.tx.tracker.core.spring.component

import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.tx.observer.common.tx.handler.TxHandler
import com.wavesenterprise.sdk.tx.tracker.api.TxTracker
import com.wavesenterprise.sdk.tx.tracker.api.TxTrackerActualTxResolver
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackStatus

class TxTrackerTxHandler(
    private val txTrackerActualTxResolver: TxTrackerActualTxResolver,
    private val txTracker: TxTracker,
) : TxHandler {
    override fun handle(tx: Tx) {
        txTrackerActualTxResolver.actualTx(tx)
            .also {
                txTracker.setTrackStatus(it, TxTrackStatus.SUCCESS)
            }
    }
}
