package com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.strategy

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.event.BlockchainEvent
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.domain.tx.TxInfo
import com.wavesenterprise.we.tx.observer.api.block.WeBlockInfo
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.HandleRollbackFactory
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.logIgnoredTxs
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.strategy.Action.HandleBlocks
import com.wavesenterprise.we.tx.observer.core.spring.executor.subscriber.toWeBlockInfo
import org.slf4j.lazyLogger
import org.slf4j.warn

class BlockAppendedEventHandlingStrategy(
    private val handleRollbackFactory: HandleRollbackFactory,
    private val appendedBlockHistoryBuffer: AppendedBlockHistoryBuffer,
) : EventHandlingStrategy {
    private val log by lazyLogger(BlockAppendedEventHandlingStrategy::class)
    private lateinit var txsFromMicroBlocks: MutableMap<String, Tx>

    init {
        clearTxsFromMicroBlocks()
    }

    private fun clearTxsFromMicroBlocks() {
        txsFromMicroBlocks = mutableMapOf()
    }

    override fun actionsOn(event: BlockchainEvent): List<Action> =
        when (event) {
            is BlockchainEvent.AppendedBlockHistory -> {
                if (appendedBlockHistoryBuffer.store(event))
                    emptyList()
                else
                    listOf((appendedBlockHistoryBuffer.clear() + event).toHandleBlocks())
            }
            is BlockchainEvent.BlockAppended -> {
                val blockAppendedWeBlockInfo = event.toWeBlockInfo(txsFromMicroBlocks)
                log.logIgnoredTxs(event.txIds.map { it.asBase58String() }, txsFromMicroBlocks)
                clearTxsFromMicroBlocks()
                val appendedBlockHistoryList = appendedBlockHistoryBuffer.clear()
                listOf(
                    HandleBlocks(
                        weBlockInfos = appendedBlockHistoryList.map { it.toWeBlockInfo() } + blockAppendedWeBlockInfo,
                        syncedBlockInfos = appendedBlockHistoryList.map { it.toSyncedBlockInfo() } + event.toSyncedBlockInfo()
                    )
                )
            }
            is BlockchainEvent.MicroBlockAppended -> {
                event.txs.forEach { tx ->
                    txsFromMicroBlocks.compute(tx.id.asBase58String()) { id, txInMap ->
                        if (txInMap != null) log.warn { "Got already received tx with id $id" }
                        tx
                    }
                }
                listOf(appendedBlockHistoryBuffer.clearAndBuildHandleBlocks())
            }
            is BlockchainEvent.RollbackCompleted -> {
                clearTxsFromMicroBlocks()
                listOf(
                    appendedBlockHistoryBuffer.clearAndBuildHandleBlocks(),
                    handleRollbackFactory.create(event)
                )
            }
        }

    companion object {
        private class BlockAppendedWeBlockInfo(
            private val blockAppended: BlockchainEvent.BlockAppended,
            private val txsFromMicroBlocks: MutableMap<String, Tx>,
        ) : WeBlockInfo {
            private val log by lazyLogger(BlockAppendedWeBlockInfo::class)
            override val height: Height
                get() = blockAppended.height
            override val txList: List<TxInfo> by lazy {
                blockAppended.txIds.asSequence()
                    .mapNotNull { id ->
                        txsFromMicroBlocks[id.asBase58String()].also { tx: Tx? ->
                            if (tx == null)
                                log.warn {
                                    "Tx with id $id was not received in MicroBlock" +
                                        ", BlockAppended [signature = '$signature', height = '$height']"
                                }
                        }
                    }
                    .map { tx ->
                        TxInfo(
                            height = height,
                            tx = tx,
                        )
                    }
                    .toList()
            }
            override val signature: Signature
                get() = blockAppended.signature
        }

        private fun BlockchainEvent.BlockAppended.toWeBlockInfo(txsFromMicroBlocks: MutableMap<String, Tx>): WeBlockInfo =
            BlockAppendedWeBlockInfo(this, txsFromMicroBlocks)
    }
}
