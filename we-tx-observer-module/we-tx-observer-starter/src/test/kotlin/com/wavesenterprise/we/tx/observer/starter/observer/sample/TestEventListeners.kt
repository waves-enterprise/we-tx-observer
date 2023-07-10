package com.wavesenterprise.we.tx.observer.starter.observer.sample

import com.wavesenterprise.sdk.node.domain.tx.AtomicTx
import com.wavesenterprise.sdk.node.domain.tx.CreatePolicyTx
import com.wavesenterprise.sdk.node.domain.tx.ExecutedContractTx
import com.wavesenterprise.sdk.node.domain.tx.PolicyDataHashTx
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.domain.tx.UpdatePolicyTx
import com.wavesenterprise.we.tx.observer.api.key.KeyEvent
import com.wavesenterprise.we.tx.observer.starter.observer.testObjects.SimpleDataObject

/**
 * Interface for convenient mocking and verifying listener methods invoked via reflection
 * TODO decompose by different TX types for better test isolation
 */
interface TestEventListeners {
    fun handleBlockEvent(tx: Tx)
    fun handleBlockEventForTxFilteredBySpEl(tx: Tx)
    fun handleBlockEventForCreateContractTxByArgumentType(createExecutedContractTx: ExecutedContractTx)
    fun handleBlockEventForCallContractTxByArgumentType(callExecutedContractTx: ExecutedContractTx)
    fun handleKeyEventWithStringValue(keyEvent: KeyEvent<String>)
    fun handleMultiKeyEventWithStringValue(keyEvent: KeyEvent<String>)
    fun handleKeyEventWithBooleanValue(keyEvent: KeyEvent<Boolean>)
    fun handleKeyEventWithIntegerValue(keyEvent: KeyEvent<Int>)
    fun handleKeyEventWithMapValue(keyEvent: KeyEvent<Map<String, List<Map<String, Int>>>>)
    fun handleKeyEventWithObjectValue(keyEvent: KeyEvent<SimpleDataObject>)
    fun handleBlockEventForTxFilteredBySpElUsingBeanResolver(tx: Tx)
    fun handleBlockEventForExecutedContractTxWithWildcard(executedContractTx: ExecutedContractTx)
    fun handleTxEventForAtomic(atomicTx: AtomicTx)
    fun handleCreatePolicyTx(createPolicyTx: CreatePolicyTx)
    fun handleUpdatePolicyTx(updatePolicyTx: UpdatePolicyTx)
    fun handlePolicyDataHashTx(policyDataHashTx: PolicyDataHashTx)
}
