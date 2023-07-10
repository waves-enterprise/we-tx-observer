package com.wavesenterprise.we.tx.observer.api.block

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.tx.TxInfo

interface WeBlockInfo {
    val height: Height
    val txList: List<TxInfo>
    val signature: Signature?
    val txCount: Long
        get() = txList.size.toLong()
}
