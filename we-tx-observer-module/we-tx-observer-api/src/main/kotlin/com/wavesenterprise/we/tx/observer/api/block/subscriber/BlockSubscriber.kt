package com.wavesenterprise.we.tx.observer.api.block.subscriber

import com.wavesenterprise.we.tx.observer.api.block.WeBlockInfo

interface BlockSubscriber {
    fun subscribe(weBlockInfo: WeBlockInfo)
}
