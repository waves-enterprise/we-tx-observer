package com.wavesenterprise.we.tx.observer.api.key

/**
 * Annotation filters the KeyEvent by the keys from the contract state.
 * @see com.wavesenterprise.we.tx.observer.api.key.KeyEvent
 * @property keyRegexp the key is in the form of a regular expression
 * @property keyPrefix the key in the form of a prefix or a full string
 */
annotation class KeyFilter(
    val keyRegexp: String = "",
    val keyPrefix: String = "",
)
