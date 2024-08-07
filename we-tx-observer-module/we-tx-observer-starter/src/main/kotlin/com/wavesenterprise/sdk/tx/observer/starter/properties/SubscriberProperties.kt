package com.wavesenterprise.sdk.tx.observer.starter.properties

import com.wavesenterprise.sdk.tx.observer.core.spring.properties.Strategy
import com.wavesenterprise.sdk.tx.observer.core.spring.properties.SubscriberProps
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.convert.DataSizeUnit
import org.springframework.util.unit.DataSize
import org.springframework.util.unit.DataUnit

@ConfigurationProperties("tx-observer.subscriber")
data class SubscriberProperties(
    override val strategy: Strategy = Strategy.BLOCK_APPENDED,
    override val blockBuffer: BlockBuffer = BlockBuffer(),
) : SubscriberProps {
    data class BlockBuffer(
        override val maxCount: Int = 100,
        @DataSizeUnit(DataUnit.BYTES)
        override val maxSize: DataSize = DataSize.ofMegabytes(10),
    ) : SubscriberProps.BlockBufferConfig
}
