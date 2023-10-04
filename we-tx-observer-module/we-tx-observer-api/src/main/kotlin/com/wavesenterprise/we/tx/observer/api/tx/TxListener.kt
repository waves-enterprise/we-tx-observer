package com.wavesenterprise.we.tx.observer.api.tx

/**
 * Listener for processing previously filtered transactions in TxEnqueuePredicate implementations.
 * The annotated @TxListener method receives transactions filtered by predicates.
 * Example:
 * ```
 * @TxListener
 * fun keyEventMyContract(
 *     @KeyFilter(keyPrefix = "EXAMPLES_") keyEvent: KeyEvent<String>
 * ) {
 *     // do something with the received data
 * }
 *
 * @TxListener
 * fun onPrivacyContainer(
 *     @PolicyFilter(namePrefix = "POLICY_NAME_") // filter by policy name
 *     @MessageFilter(metaKey = "key", metaKeyValue = "value") // additional filtering parameter by the comment field
 *     privateDataEvent: PrivateDataEvent<String>,
 * ) {
 *     // payload from PrivateDataEvent parameterized
 *     // do something with the received private data
 * }
 * ```
 * @property filterExpression filter transaction by spel expression
 * @see com.wavesenterprise.we.tx.observer.api.tx.TxEnqueuePredicate
 * @see com.wavesenterprise.we.tx.observer.api.key.KeyEvent
 * @see com.wavesenterprise.we.tx.observer.api.privacy.PolicyFilter
 * @see com.wavesenterprise.we.tx.observer.api.privacy.MessageFilter
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TxListener(
    val filterExpression: String = "",
)
