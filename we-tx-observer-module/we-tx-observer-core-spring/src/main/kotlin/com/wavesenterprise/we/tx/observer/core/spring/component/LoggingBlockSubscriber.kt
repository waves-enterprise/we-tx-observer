package com.wavesenterprise.we.tx.observer.core.spring.component

import com.wavesenterprise.we.tx.observer.api.block.WeBlockInfo
import com.wavesenterprise.we.tx.observer.api.block.subscriber.BlockSubscriber
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LoggingBlockSubscriber : BlockSubscriber {

    val logger: Logger = LoggerFactory.getLogger(LoggingBlockSubscriber::class.java)

    override fun subscribe(weBlockInfo: WeBlockInfo) {
        weBlockInfo.apply {
            logger.debug("Block $height read with signature = $signature and txCount = $txCount")
        }
    }
}
