package com.wavesenterprise.sdk.tx.observer.api.block.subscriber

import com.wavesenterprise.sdk.tx.observer.api.block.WeRollbackInfo

interface RollbackSubscriber {
    fun onRollback(weRollbackInfo: WeRollbackInfo)
}
