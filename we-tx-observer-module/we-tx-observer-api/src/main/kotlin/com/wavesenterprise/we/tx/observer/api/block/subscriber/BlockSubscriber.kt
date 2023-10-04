package com.wavesenterprise.we.tx.observer.api.block.subscriber

import com.wavesenterprise.we.tx.observer.api.block.WeBlockInfo

/**
 * Subscriber handling a new block with transactions. Can be invoked multiple times for a liquid block.
 */
interface BlockSubscriber {
    /**
     * Sends transactions from the received block to the queue.
     * @param weBlockInfo block with transactions
     * @see WeBlockInfo
     */
    fun subscribe(weBlockInfo: WeBlockInfo)
}
