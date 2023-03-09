package com.wavesenterprise.we.tx.observer.component

import com.wavesenterprise.we.tx.observer.model.WeRollbackInfo

interface RollbackSubscriber {
    fun onRollback(rollbackInfo: WeRollbackInfo)
}
