package com.wavesenterprise.sdk.tx.tracker.api

import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.sdk.node.domain.contract.ContractTxStatus
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackBusinessObjectInfo
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackInfo
import com.wavesenterprise.sdk.tx.tracker.domain.TxTrackStatus
import org.springframework.data.domain.PageRequest
import java.util.Optional

/**
 * Service for tracking, managing and obtaining up-to-date information about necessary transactions.
 */
interface TxTracker {

    /**
     * Add a transaction to tracking.
     * @param tx transaction
     * @param meta additional information for tx
     * @param businessObjectInfos list of business objects for transaction
     * @param userId ID of the user who added the transaction to the tracking
     * @return TxTrackInfo - domain object about tracking transaction
     * @see com.wavesenterprise.sdk.tx.tracker.domain.TxTrackInfo
     */
    fun trackTx(
        tx: Tx,
        meta: Map<String, Any> = mapOf(),
        businessObjectInfos: List<TxTrackBusinessObjectInfo> = listOf(),
        userId: String? = null,
    ): TxTrackInfo

    /**
     * Change the status of a tracked transaction.
     * @param tx transaction
     * @param status new tracked transaction status
     */
    fun setTrackStatus(tx: Tx, status: TxTrackStatus)

    /**
     * Check the existence of a transaction in tracking.
     * @param tx transaction
     */
    fun existsInTracker(tx: Tx): Boolean

    /**
     * Check the existence of a transaction in tracking with status.
     * @param tx transaction
     * @param txTrackerStatus status
     */
    fun existsInTrackerWithStatus(tx: Tx, txTrackerStatus: TxTrackStatus): Boolean

    /**
     * Get a list of transactions by status.
     * @param txTrackerStatus status
     * @return list of Tx
     */
    fun getTrackedTxWithStatus(txTrackerStatus: TxTrackStatus): List<Tx>

    /**
     * Get a list ids of transactions by status.
     * @param txTrackerStatus status
     * @return list of txIds
     */
    fun getTrackedTxIdsWithStatus(txTrackerStatus: TxTrackStatus): List<TxId>

    /**
     * Get a list ids of transactions by status and types.
     * @param txTrackerStatus status
     * @param types types of transactions
     * @param pageRequest request for pagination
     * @return list of txIds
     */
    fun getTrackedTxIdsWithStatusAndTypes(
        txTrackerStatus: TxTrackStatus,
        types: List<Int>,
        pageRequest: PageRequest,
    ): List<TxId>

    /**
     * Get a list of transactions by status.
     * @param txTrackerStatus status
     * @param pageRequest request for pagination
     * @return list of txs
     */
    fun getTrackedTxsWithStatus(
        txTrackerStatus: TxTrackStatus,
        pageRequest: PageRequest,
    ): List<Tx>

    /**
     * Get last unsuccessful(ERROR, FAILURE statuses) TxTrackInfo by id of business object.
     * @param businessObjectId id of business object
     * @return optional TxTrackInfo
     */
    fun getLastUnsuccessfulTrackedTxForBusinessObject(
        businessObjectId: String,
    ): Optional<TxTrackInfo>

    /**
     * Get last tracked TxTrackInfo by id of business object and transaction status.
     * @param businessObjectId id of business object
     * @param status status of tracked transaction
     * @return optional TxTrackInfo
     */
    fun getLastTrackedTxForBusinessObjectWithStatus(
        businessObjectId: String,
        status: TxTrackStatus,
    ): Optional<TxTrackInfo>

    /**
     * Establish the transaction status of a contract.
     * @param txId id transactions of contract
     * @param contractTxStatusDtoList
     */
    fun setContractTxError(txId: TxId, contractTxStatusDtoList: List<ContractTxStatus>)
}
