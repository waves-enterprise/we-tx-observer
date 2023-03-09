package com.wavesenterprise.we.tx.observer.model

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.blocks.BlockAtHeight
import com.wavesenterprise.sdk.node.domain.tx.Tx

data class WeHttpApiBlockInfo(
    private val blocksAtHeightDto: BlockAtHeight
) : WeBlockInfo {
    override val height: Height
        get() = blocksAtHeightDto.height
    override val txList: List<Tx>
        get() = blocksAtHeightDto.transactions
    override val signature: Signature
        get() = blocksAtHeightDto.signature
    override val txCount: Long
        get() = blocksAtHeightDto.transactionCount
}
