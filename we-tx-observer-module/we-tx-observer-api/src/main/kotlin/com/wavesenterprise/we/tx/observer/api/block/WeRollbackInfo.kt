package com.wavesenterprise.we.tx.observer.api.block

import com.wavesenterprise.sdk.node.domain.Height
import com.wavesenterprise.sdk.node.domain.Signature
import com.wavesenterprise.sdk.node.domain.TxId

interface WeRollbackInfo {
    val toHeight: Height
    val toBlockSignature: Signature
    val rollbackTxIds: Collection<TxId>
}
