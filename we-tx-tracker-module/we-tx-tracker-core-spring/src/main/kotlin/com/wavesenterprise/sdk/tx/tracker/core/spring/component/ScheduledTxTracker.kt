package com.wavesenterprise.sdk.tx.tracker.core.spring.component

import com.wavesenterprise.sdk.node.client.blocking.contract.ContractService
import com.wavesenterprise.sdk.node.client.blocking.node.NodeBlockingServiceFactory
import com.wavesenterprise.sdk.node.client.blocking.tx.TxService
import com.wavesenterprise.sdk.node.domain.Timestamp.Companion.toDateTimeFromUTCBlockChain
import com.wavesenterprise.sdk.node.domain.TxType
import com.wavesenterprise.sdk.node.domain.tx.AtomicTx
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.domain.tx.Tx.Companion.type
import com.wavesenterprise.sdk.tx.tracker.api.TxTracker
import com.wavesenterprise.sdk.tx.tracker.core.spring.properties.TxTrackerProps
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackStatus
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.data.domain.PageRequest
import java.time.OffsetDateTime

open class ScheduledTxTracker(
    val nodeBlockingServiceFactory: NodeBlockingServiceFactory,
    val txTracker: TxTracker,
    val txTrackerProperties: TxTrackerProps,
) {
    private val txService: TxService = nodeBlockingServiceFactory.txService()
    private val contractService: ContractService = nodeBlockingServiceFactory.contractService()

    @SchedulerLock(
        name = "trackPendingTx_task",
        lockAtLeastFor = "\${tx-tracker.lock-at-least:PT0S}",
        lockAtMostFor = "\${tx-tracker.lock-at-most:PT1M}",
    )
    open fun trackPendingTx() {
        val trackedTxs = txTracker.getTrackedTxsWithStatus(
            txTrackerStatus = TxTrackStatus.PENDING,
            pageRequest = PageRequest.of(0, txTrackerProperties.trackedTxPageRequestLimit)
        )
        if (trackedTxs.isNotEmpty()) {
            val unconfirmedTxIds = txService.utxInfo()
                .asSequence()
                .flatMap {
                    when (it) {
                        is AtomicTx -> sequenceOf(it) + it.txs
                        else -> sequenceOf(it)
                    }
                }
                .map { it.id }
                .toSet()
            val contractTxTypes = listOf(TxType.CALL_CONTRACT, TxType.CREATE_CONTRACT)

            trackedTxs.filter { tx -> tx.id !in unconfirmedTxIds }
                .forEach { confirmedOrLostTx ->
                    confirmedOrLostTx.updateMinedTxStatus()
                        ?: if (confirmedOrLostTx.timeoutReached()) {
                            confirmedOrLostTx.setTrackStatus(TxTrackStatus.FAILURE)
                        } else if (confirmedOrLostTx.type() in contractTxTypes) {
                            confirmedOrLostTx.updateContractTxStatus()
                        }
                }
        }
    }

    private fun Tx.updateMinedTxStatus(): Unit? =
        if (txService.txInfo(id).isPresent) setTrackStatus(TxTrackStatus.SUCCESS) else null

    private fun Tx.timeoutReached() =
        timestamp.toDateTimeFromUTCBlockChain().plus(txTrackerProperties.timeout) < OffsetDateTime.now()

    private fun Tx.setTrackStatus(status: TxTrackStatus) {
        txTracker.setTrackStatus(this, status)
    }

    private fun Tx.updateContractTxStatus() {
        // todo try catch
        val contractTxStatus = contractService.getContractTxStatus(id)
        if (contractTxStatus.isNotEmpty()) {
            txTracker.setContractTxError(id, contractTxStatus)
        }
    }
}
