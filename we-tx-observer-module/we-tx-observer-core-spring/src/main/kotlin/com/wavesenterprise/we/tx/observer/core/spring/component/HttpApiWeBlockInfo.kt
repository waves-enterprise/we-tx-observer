package com.wavesenterprise.we.tx.observer.core.spring.component

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.blocks.BlockAtHeight
import com.wavesenterprise.sdk.node.domain.tx.TxInfo
import com.wavesenterprise.we.tx.observer.api.block.WeBlockInfo

data class HttpApiWeBlockInfo(
    private val blocksAtHeight: BlockAtHeight,
) : WeBlockInfo {
    override val height: Height
        get() = blocksAtHeight.height
    override val txList: List<TxInfo>
        get() = blocksAtHeight.transactions.map { tx ->
            TxInfo(
                height = blocksAtHeight.height,
                tx = tx,
            )
        }
    override val signature: Signature
        get() = blocksAtHeight.signature
    override val txCount: Long
        get() = blocksAtHeight.transactionCount
}
