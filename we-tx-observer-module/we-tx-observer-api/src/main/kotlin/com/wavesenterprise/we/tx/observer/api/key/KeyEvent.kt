package com.wavesenterprise.we.tx.observer.api.key

import com.wavesenterprise.sdk.node.domain.tx.ExecutedContractTx

data class KeyEvent<T>(
    val payload: T,
    val tx: ExecutedContractTx,
    val key: String,
    val contractId: String,
)
