package com.wavesenterprise.sdk.tx.observer.starter.observer.sample

import com.wavesenterprise.sdk.node.domain.tx.AtomicTx
import com.wavesenterprise.sdk.node.domain.tx.CreatePolicyTx
import com.wavesenterprise.sdk.node.domain.tx.ExecutedContractTx
import com.wavesenterprise.sdk.node.domain.tx.PolicyDataHashTx
import com.wavesenterprise.sdk.node.domain.tx.Tx
import com.wavesenterprise.sdk.node.domain.tx.UpdatePolicyTx
import com.wavesenterprise.sdk.tx.observer.api.key.KeyEvent
import com.wavesenterprise.sdk.tx.observer.api.key.KeyFilter
import com.wavesenterprise.sdk.tx.observer.api.tx.TxListener
import com.wavesenterprise.sdk.tx.observer.starter.observer.testObjects.SimpleDataObject

class TestBlockListeners(
    val mockedTestEventListeners: TestEventListeners
) : TestEventListeners {

    @TxListener
    override fun handleBlockEvent(tx: Tx) {
        mockedTestEventListeners.handleBlockEvent(tx)
    }

    @TxListener
    override fun handleBlockEventForExecutedContractTxWithWildcard(executedContractTx: ExecutedContractTx) {
        mockedTestEventListeners.handleBlockEventForExecutedContractTxWithWildcard(executedContractTx)
    }

    @TxListener
    override fun handleBlockEventForCallContractTxByArgumentType(
        callExecutedContractTx: ExecutedContractTx
    ) {
        mockedTestEventListeners.handleBlockEventForCallContractTxByArgumentType(callExecutedContractTx)
    }

    @TxListener
    override fun handleBlockEventForCreateContractTxByArgumentType(
        createExecutedContractTx: ExecutedContractTx
    ) {
        mockedTestEventListeners.handleBlockEventForCreateContractTxByArgumentType(createExecutedContractTx)
    }

    @TxListener
    override fun handleKeyEventWithStringValue(
        @KeyFilter(keyPrefix = "my_fav_object_string") keyEvent: KeyEvent<String>
    ) {
        mockedTestEventListeners.handleKeyEventWithStringValue(keyEvent)
    }

    @TxListener
    override fun handleMultiKeyEventWithStringValue(
        @KeyFilter(keyPrefix = "my_fav_multi_key_string") keyEvent: KeyEvent<String>
    ) {
        mockedTestEventListeners.handleMultiKeyEventWithStringValue(keyEvent)
    }

    @TxListener
    override fun handleKeyEventWithIntegerValue(
        @KeyFilter(keyPrefix = "my_fav_key_int_") keyEvent: KeyEvent<Int>
    ) {
        mockedTestEventListeners.handleKeyEventWithIntegerValue(keyEvent)
    }

    @TxListener
    override fun handleKeyEventWithBooleanValue(
        @KeyFilter(keyPrefix = "my_fav_object_bool_") keyEvent: KeyEvent<Boolean>
    ) {
        mockedTestEventListeners.handleKeyEventWithBooleanValue(keyEvent)
    }

    @TxListener
    override fun handleKeyEventWithMapValue(
        @KeyFilter(keyPrefix = "my_fav_object_map_") keyEvent: KeyEvent<Map<String, List<Map<String, Int>>>>
    ) {
        mockedTestEventListeners.handleKeyEventWithMapValue(keyEvent)
    }

    @TxListener
    override fun handleKeyEventWithObjectValue(
        @KeyFilter(keyPrefix = "my_fav_object_object_") keyEvent: KeyEvent<SimpleDataObject>
    ) {
        mockedTestEventListeners.handleKeyEventWithObjectValue(keyEvent)
    }

    @TxListener
    (
        filterExpression = "T(com.wavesplatform.vst.node.dto.TransactionType).EXECUTED_CONTRACT.value == type " +
            "&& T(com.wavesplatform.vst.node.dto.TransactionType).CREATE_CONTRACT.value == tx.type " +
            "&& 'image_to_filter' == tx.image"
    )
    override fun handleBlockEventForTxFilteredBySpEl(tx: Tx) {
        mockedTestEventListeners.handleBlockEventForTxFilteredBySpEl(tx)
    }

    @TxListener(filterExpression = "@customConditionBean.checkTx(#root)")
    override fun handleBlockEventForTxFilteredBySpElUsingBeanResolver(tx: Tx) {
        mockedTestEventListeners.handleBlockEventForTxFilteredBySpElUsingBeanResolver(tx)
    }

    @TxListener
    override fun handleCreatePolicyTx(createPolicyTx: CreatePolicyTx) {
        mockedTestEventListeners.handleCreatePolicyTx(createPolicyTx)
    }

    @TxListener
    override fun handleUpdatePolicyTx(updatePolicyTx: UpdatePolicyTx) {
        mockedTestEventListeners.handleUpdatePolicyTx(updatePolicyTx)
    }

    @TxListener
    override fun handlePolicyDataHashTx(policyDataHashTx: PolicyDataHashTx) {
        mockedTestEventListeners.handlePolicyDataHashTx(policyDataHashTx)
    }

    @TxListener
    override fun handleTxEventForAtomic(atomicTx: AtomicTx) {
        mockedTestEventListeners.handleTxEventForAtomic(atomicTx)
    }
}
