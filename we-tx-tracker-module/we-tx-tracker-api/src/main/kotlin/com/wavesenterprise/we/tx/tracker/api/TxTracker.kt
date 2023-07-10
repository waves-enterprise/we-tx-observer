package com.wavesenterprise.we.tx.tracker.api

import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.contract.ContractTxStatus
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.we.tx.tracker.domain.TxTrackBusinessObjectInfo
import com.wavesenterprise.we.tx.tracker.domain.TxTrackInfo
import com.wavesenterprise.we.tx.tracker.domain.TxTrackStatus
import org.springframework.data.domain.PageRequest
import java.util.UUID

interface TxTracker {
    fun trackTx(
        tx: Tx,
        meta: Map<String, Any> = mapOf(),
        businessObjectInfos: List<TxTrackBusinessObjectInfo> = listOf(),
        userId: UUID? = null,
    ): TxTrackInfo

    fun setTrackStatus(tx: Tx, status: TxTrackStatus)
    fun existsInTracker(tx: Tx): Boolean
    fun existsInTrackerWithStatus(tx: Tx, txTrackerStatus: TxTrackStatus): Boolean
    fun getTrackedTxWithStatus(txTrackerStatus: TxTrackStatus): List<Tx>
    fun getTrackedTxIdsWithStatus(txTrackerStatus: TxTrackStatus): List<TxId>

    fun getTrackedTxIdsWithStatusAndTypes(
        txTrackerStatus: TxTrackStatus,
        types: List<Int>,
        pageRequest: PageRequest
    ): List<TxId>

    fun getTrackedTxsWithStatus(
        txTrackerStatus: TxTrackStatus,
        pageRequest: PageRequest
    ): List<Tx>

    fun getLastUnsuccessfulTrackedTxForBusinessObject(businessObjectId: String): TxTrackInfo?
    fun setContractTxError(txId: TxId, contractTxStatusDtoList: List<ContractTxStatus>)
}
