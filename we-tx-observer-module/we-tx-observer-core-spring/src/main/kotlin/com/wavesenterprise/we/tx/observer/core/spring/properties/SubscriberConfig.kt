package com.wavesenterprise.we.tx.observer.core.spring.properties

import org.springframework.util.unit.DataSize

interface SubscriberProps {
    val strategy: Strategy
    val blockBuffer: BlockBufferConfig

    interface BlockBufferConfig {
        val maxCount: Int
        val maxSize: DataSize
    }
}

enum class Strategy {
    MICRO_BLOCK,
    BLOCK_APPENDED,
}
