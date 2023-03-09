package com.wavesenterprise.we.tx.observer.model

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.tx.Tx

interface WeBlockInfo {
    val height: Height
    val txList: List<Tx>
    val signature: Signature
    val txCount: Long
        get() = txList.size.toLong()
}
