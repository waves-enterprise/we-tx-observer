package com.wavesenterprise.we.tx.observer.api.privacy

annotation class MessageFilters(
    vararg val filters: MessageFilter = []
)

annotation class MessageFilter(
    val metaKey: String = "",
    val metaKeyValue: String = "",
    val metaKeyValueRegExp: String = "",
)
