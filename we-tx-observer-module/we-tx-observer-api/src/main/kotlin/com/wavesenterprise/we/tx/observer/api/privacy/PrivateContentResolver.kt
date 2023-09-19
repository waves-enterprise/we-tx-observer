package com.wavesenterprise.we.tx.observer.api.privacy

import com.fasterxml.jackson.databind.JsonNode
import com.wavesenterprise.sdk.node.domain.tx.CreatePolicyTx
import com.wavesenterprise.sdk.node.domain.tx.PolicyDataHashTx
import java.lang.reflect.Type

interface PrivateContentResolver {

    fun <T> decode(tx: PolicyDataHashTx, type: Type): PrivateDataEvent<T>
    fun decode(tx: PolicyDataHashTx): PrivateDataEvent<Nothing>
    fun getMeta(tx: PolicyDataHashTx): JsonNode?
    fun getPolicyTx(tx: PolicyDataHashTx): CreatePolicyTx
    fun getPolicyName(tx: PolicyDataHashTx): String = getPolicyTx(tx).policyName.value
    fun isAvailable(tx: PolicyDataHashTx): Boolean
}
