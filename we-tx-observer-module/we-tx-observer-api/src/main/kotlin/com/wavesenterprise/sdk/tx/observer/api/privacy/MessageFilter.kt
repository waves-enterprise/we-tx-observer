package com.wavesenterprise.sdk.tx.observer.api.privacy

/**
 * Annotation containing an array of filters.
 * @property filters array of MessageFilter
 * @see com.wavesenterprise.sdk.tx.observer.api.privacy.MessageFilter
 */
annotation class MessageFilters(
    vararg val filters: MessageFilter = [],
)

/**
 * Annotation which filters privacy data by comment field (in json format).
 * @property metaKey filter in the form of a key of json
 * @property metaKeyValue filter in the form of a value of json;
 * @property metaKeyValueRegExp filter in the form of a regular expression in json;
 */
annotation class MessageFilter(
    val metaKey: String = "",
    val metaKeyValue: String = "",
    val metaKeyValueRegExp: String = "",
)
