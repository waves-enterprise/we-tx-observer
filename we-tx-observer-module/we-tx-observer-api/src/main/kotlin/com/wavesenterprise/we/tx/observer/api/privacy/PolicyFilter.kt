package com.wavesenterprise.we.tx.observer.api.privacy

/**
 * Annotation which filters privacy data by policy name.
 * @property namePrefix the key in the form of a prefix or a full string
 * @property nameRegExp the key is in the form of a regular expression
 */
annotation class PolicyFilter(
    val namePrefix: String = "",
    val nameRegExp: String = "",
)
