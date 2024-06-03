package com.wavesenterprise.we.tx.observer.core.spring.web.service

import com.wavesenterprise.sdk.node.domain.TxId
import com.wavesenterprise.we.tx.observer.core.spring.web.dto.PatchTxApiDto
import com.wavesenterprise.we.tx.observer.core.spring.web.dto.QueueStatusApiDto
import com.wavesenterprise.we.tx.observer.domain.EnqueuedTx

interface TxQueueService {
    fun getQueueStatus(): QueueStatusApiDto
    fun addTxToQueueById(txId: TxId): EnqueuedTx
    fun changeTxStatusInQueue(txId: String, patchTxDto: PatchTxApiDto): EnqueuedTx
    fun deleteTxFromQueue(txId: String)
    fun deleteForked(): Int
    fun getTxById(txId: TxId): EnqueuedTx
    fun resetToHeightAsynchronously(blockHeight: Long)
    fun postponeErrors(): Int
}
