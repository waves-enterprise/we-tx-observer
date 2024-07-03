package com.wavesenterprise.sdk.tx.observer.api.privacy

import com.fasterxml.jackson.databind.JsonNode
import com.wavesenterprise.sdk.node.domain.tx.CreatePolicyTx
import com.wavesenterprise.sdk.node.domain.tx.PolicyDataHashTx
import com.wavesenterprise.sdk.tx.observer.api.NoPayloadException

/**
 * Class processed in @TxListener methods for privacy transactions (112, 113, 114)
 * @property payload parametrized private data
 * @property meta additional(comment) information by privacy data in JsonNode format
 * @property policyName name of policy
 * @property createPolicyTx policy creation transaction (112)
 * @property policyDataHashTx transaction of sending private data to policy (114)
 */
data class PrivateDataEvent<T> private constructor(
    private val payloadInitializer: () -> T,
    val meta: JsonNode?,
    val policyName: String,
    val createPolicyTx: CreatePolicyTx,
    val policyDataHashTx: PolicyDataHashTx,
) {
    constructor(
        meta: JsonNode?,
        policyName: String,
        createPolicyTx: CreatePolicyTx,
        policyDataHashTx: PolicyDataHashTx,
    ) : this(
        payloadInitializer = { throw NoPayloadException() },
        meta = meta,
        policyName = policyName,
        createPolicyTx = createPolicyTx,
        policyDataHashTx = policyDataHashTx,
    )

    constructor(
        payload: T,
        meta: JsonNode?,
        policyName: String,
        createPolicyTx: CreatePolicyTx,
        policyDataHashTx: PolicyDataHashTx,
    ) : this(
        payloadInitializer = { payload },
        meta = meta,
        policyName = policyName,
        createPolicyTx = createPolicyTx,
        policyDataHashTx = policyDataHashTx,
    )

    val payload: T by lazy(payloadInitializer)
}
