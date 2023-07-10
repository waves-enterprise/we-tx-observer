package com.wavesenterprise.we.tx.observer.core.spring.util

import com.wavesenterprise.sdk.node.domain.tx.ExecutedContractTx
import java.util.concurrent.ConcurrentHashMap

class TxSpElUtils {

    companion object {

        private val regExpCache: ConcurrentHashMap<String, Regex> = ConcurrentHashMap()

        @JvmStatic
        fun hasKeyWithRegex(txExecuted: ExecutedContractTx, regExp: String): Boolean {
            val compiledRegex = regExpCache.getOrPut(regExp) { Regex(regExp) }
            return txExecuted.results.any { it.key.value.matches(compiledRegex) }
        }

        @JvmStatic
        fun hasKeyWithPrefix(txExecuted: ExecutedContractTx, prefix: String): Boolean =
            hasKeyWithRegex(txExecuted, "$prefix.*")
        // todo check action of smarContract

        @JvmStatic
        fun isMatchRegex(value: String, regExp: String): Boolean {
            val compiledRegex = regExpCache.getOrPut(regExp) { Regex(regExp) }
            return value.matches(compiledRegex)
        }

        @JvmStatic
        fun isWithPrefix(value: String, prefix: String): Boolean =
            isMatchRegex(value, "$prefix.*")
    }
}
