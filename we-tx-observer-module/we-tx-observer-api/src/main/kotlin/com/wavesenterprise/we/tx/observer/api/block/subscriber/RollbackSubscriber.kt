package com.wavesenterprise.we.tx.observer.api.block.subscriber

import com.wavesenterprise.we.tx.observer.api.block.WeRollbackInfo

interface RollbackSubscriber {
    fun onRollback(weRollbackInfo: WeRollbackInfo)
}
