package com.wavesenterprise.sdk.tx.observer.api.block

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.tx.TxInfo

/**
 * Block info interface
 * @property height block height
 * @property txList transaction list
 * @property signature block signature
 * @property txCount quantity transaction
 */
interface WeBlockInfo {
    val height: Height
    val txList: List<TxInfo>
    val signature: Signature?
    val txCount: Long
        get() = txList.size.toLong()
}
