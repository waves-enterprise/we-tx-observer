package com.wavesenterprise.sdk.tx.observer.api.key

import com.wavesenterprise.sdk.node.domain.tx.ExecutedContractTx

/**
 * Class processed in @TxListener methods for 105 transaction.
 * @see com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
 * @property payload parametrized object obtained from the results of transaction ExecutedContractTx (105)
 * @property tx transaction ExecutedContractTx (105)
 * @property key key by which filtering takes place in KeyFilter
 * @property contractId identifier of the contract being invoked
 */
data class KeyEvent<T>(
    val payload: T,
    val tx: ExecutedContractTx,
    val key: String,
    val contractId: String,
)
