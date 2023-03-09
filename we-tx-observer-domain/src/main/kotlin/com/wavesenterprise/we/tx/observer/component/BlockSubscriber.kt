package com.wavesenterprise.we.tx.observer.component

import com.wavesenterprise.we.tx.observer.model.WeBlockInfo

interface BlockSubscriber {
    fun subscribe(blockInfo: WeBlockInfo)
}
