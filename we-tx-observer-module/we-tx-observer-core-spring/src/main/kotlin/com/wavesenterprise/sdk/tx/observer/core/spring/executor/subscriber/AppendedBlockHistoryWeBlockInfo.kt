package com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.event.BlockchainEvent
import com.wavesenterprise.sdk.node.domain.tx.TxInfo
import com.wavesenterprise.sdk.tx.observer.api.block.WeBlockInfo

class AppendedBlockHistoryWeBlockInfo(
    private val appendedBlockHistory: BlockchainEvent.AppendedBlockHistory,
) : WeBlockInfo {
    override val height: Height
        get() = appendedBlockHistory.height
    override val txList: List<TxInfo>
        get() = appendedBlockHistory.txs.map { tx ->
            TxInfo(height = height, tx = tx)
        }
    override val signature: Signature
        get() = appendedBlockHistory.signature
}

fun BlockchainEvent.AppendedBlockHistory.toWeBlockInfo() =
    AppendedBlockHistoryWeBlockInfo(this)
