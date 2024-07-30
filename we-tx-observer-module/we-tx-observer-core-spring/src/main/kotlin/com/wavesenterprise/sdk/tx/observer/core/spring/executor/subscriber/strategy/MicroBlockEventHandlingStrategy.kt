package com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber.strategy

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.event.BlockchainEvent
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.domain.tx.TxInfo
import com.wavesenterprise.sdk.tx.observer.api.block.WeBlockInfo
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber.HandleRollbackFactory
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber.logIgnoredTxs
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber.strategy.Action.HandleBlocks
import com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber.toWeBlockInfo
import org.slf4j.lazyLogger
import org.slf4j.warn

class MicroBlockEventHandlingStrategy(
    private val handleRollbackFactory: HandleRollbackFactory,
    private val appendedBlockHistoryBuffer: AppendedBlockHistoryBuffer,
    private var height: Long,
) : EventHandlingStrategy {
    private val log by lazyLogger(MicroBlockEventHandlingStrategy::class)
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
                height = event.height.value + 1
                if (appendedBlockHistoryBuffer.store(event)) {
                    emptyList()
                } else {
                    listOf((appendedBlockHistoryBuffer.clear() + event).toHandleBlocks())
                }
            }

            is BlockchainEvent.BlockAppended -> {
                height = event.height.value + 1
                log.logIgnoredTxs(event.txIds.map { it.asBase58String() }, txsFromMicroBlocks)
                clearTxsFromMicroBlocks()
                val appendedBlockHistoryList = appendedBlockHistoryBuffer.clear()
                listOf(
                    HandleBlocks(
                        weBlockInfos = appendedBlockHistoryList.map { it.toWeBlockInfo() },
                        syncedBlockInfos =
                        appendedBlockHistoryList.map { it.toSyncedBlockInfo() } + event.toSyncedBlockInfo(),
                    ),
                )
            }

            is BlockchainEvent.MicroBlockAppended -> {
                val appendedBlockHistoryList = appendedBlockHistoryBuffer.clear()
                listOf(
                    HandleBlocks(
                        weBlockInfos =
                        appendedBlockHistoryList
                            .map { it.toWeBlockInfo() } + event.toWeBlockInfo(Height(height))
                            .also {
                                event.txs.forEach { tx ->
                                    txsFromMicroBlocks.compute(tx.id.asBase58String()) { id, txInMap ->
                                        if (txInMap != null) log.warn { "Got already received tx with id $id" }
                                        tx
                                    }
                                }
                            },
                        syncedBlockInfos = appendedBlockHistoryList.map { it.toSyncedBlockInfo() },
                    ),
                )
            }

            is BlockchainEvent.RollbackCompleted -> listOf(
                appendedBlockHistoryBuffer.clearAndBuildHandleBlocks(),
                handleRollbackFactory.create(event).also {
                    height = it.weRollbackInfo.toHeight.value
                },
            )
        }

    companion object {
        private class MicroBlockAppendedWeBlockInfo(
            private val microBlockAppended: BlockchainEvent.MicroBlockAppended,
            override val height: Height,
        ) : WeBlockInfo {
            override val txList: List<TxInfo> by lazy {
                microBlockAppended.txs.map { tx ->
                    TxInfo(
                        height = height,
                        tx = tx,
                    )
                }
            }
            override val signature: Signature? = null
        }

        private fun BlockchainEvent.MicroBlockAppended.toWeBlockInfo(height: Height): WeBlockInfo =
            MicroBlockAppendedWeBlockInfo(this, height)
    }
}
