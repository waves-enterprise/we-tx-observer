package com.wavesenterprise.we.tx.observer.model

interface WeRollbackInfo {
    val toHeight: Long
    val toBlockSignature: String
    val rollbackTxIds: Collection<String>
}
