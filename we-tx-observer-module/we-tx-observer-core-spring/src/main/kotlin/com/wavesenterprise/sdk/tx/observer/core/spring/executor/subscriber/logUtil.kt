package com.wavesenterprise.sdk.tx.observer.core.spring.executor.subscriber

import com.wavesenterprise.sdk.node.domain.tx.Tx
import org.slf4j.KLogger
import org.slf4j.debug
import org.slf4j.trace

internal fun KLogger.logIgnoredTxs(
    appendedTxIds: List<String>,
    txsFromMicroBlocks: MutableMap<String, Tx>,
) {
    val ignoredTxs by lazy {
        val appendedTxIdsSet = appendedTxIds.toSet()
        txsFromMicroBlocks.asSequence()
            .filter { (id, _) ->
                id in appendedTxIdsSet
            }
            .map { (_, tx) -> tx }
            .toList()
    }
    debug { "Ignored tx ids: ${ignoredTxs.map { it.id }}" }
    trace { "Ignored txs: $ignoredTxs" }
}
